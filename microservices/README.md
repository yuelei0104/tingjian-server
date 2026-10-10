# Tingjian 渐进式微服务架构

这一目录提供与现有单体后端并行的微服务运行面。根目录中的 Spring Boot 应用仍然是当前稳定业务实现，迁移期间不删除、不改端口，也不要求 Android 立即切换接口。

## 第一阶段服务

| 服务 | 默认端口 | 当前职责 |
| --- | ---: | --- |
| `gateway-service` | 8088 | 统一入口、鉴权边界、HTTP/WebSocket 路由、请求链路 ID |
| `ai-speech-service` | 8092 | 单 Agent 编排、Qwen 多轮回复/流式输出、会话总结及语音提供商边界 |
| `usage-service` | 8093 | 套餐、日配额、持久化预占/确认/释放和幂等处理 |
| 根目录单体应用 | 8080 | 账号、会话、历史、个性化等现有稳定业务 |

网关只把公开的 `/api/**` 和 `/ws/**` 转发给现有单体。`/internal/**` 不再暴露在网关上，
服务之间通过内网地址直连并使用 `X-Internal-Service-Token` 认证。因此 Android 只需要把
API 基地址改为网关地址，不需要也不允许调用内部接口。

## 为什么先并行而不是直接拆库

- 现有功能和测试继续运行，迁移失败时可直接绕过网关回到 8080。
- 先建立服务边界、内部契约和用量控制，再逐个迁移实现，避免一次改动账号、会话和语音的所有数据。
- `common-contract` 只存放跨服务 DTO/枚举，不依赖任何业务服务，避免模块相互引用。
- 第一阶段用静态 URL 或 Docker DNS 路由；服务注册中心要等技术栈版本验证后再接入。

## 本地验证

先验证原有单体：

```powershell
.\mvnw.cmd test
```

再验证微服务聚合工程：

```powershell
.\mvnw.cmd -f .\microservices\pom.xml test
```

打包后，在三个终端分别启动服务：

```powershell
.\mvnw.cmd -f .\microservices\pom.xml package
java -jar .\microservices\gateway-service\target\gateway-service-0.1.0-SNAPSHOT.jar
java -jar .\microservices\ai-speech-service\target\ai-speech-service-0.1.0-SNAPSHOT.jar
java -jar .\microservices\usage-service\target\usage-service-0.1.0-SNAPSHOT.jar
```

环境变量可覆盖服务地址：

| 环境变量 | 默认值 |
| --- | --- |
| `TINGJIAN_LEGACY_HTTP_URL` | `http://127.0.0.1:8080` |
| `TINGJIAN_LEGACY_WS_URL` | `ws://127.0.0.1:8080` |

网关诊断地址为 `http://127.0.0.1:8088/gateway/status`。访问 `/api/v1/**` 或 `/ws/**`
时必须携带 Bearer Token；真正的令牌有效性仍由账号服务校验。网关会删除客户端伪造的
`X-Internal-Service-Token`，下游不可达时统一返回 `DOWNSTREAM_UNAVAILABLE`（HTTP 503）。

## 迁移顺序

1. Android 真机只切换到网关 8088，验证旧接口和 WebSocket 都可透传。
2. 在调用云端 ASR/AI 前向 `usage-service` 预占用量，成功后确认，失败时释放。
3. AI 表达、多轮 Agent 和大模型会话总结由 `ai-speech-service` 执行；单体负责用户鉴权、
   数据隔离、只读工具快照和用量预占，对 Android 保持公开契约稳定。
4. 再拆账号与会话服务；到这一步才分别迁移数据库表和引入可靠事件/消息队列。

`usage-service` 使用 MySQL 表 `usage_user_plan`、`usage_daily_bucket` 和
`usage_reservation` 保存套餐、每日桶和预占记录；独立的
`usage_flyway_schema_history` 不会与单体迁移记录冲突。Redis 只缓存幂等键到预占编号的
索引，Redis 不可用时自动回退 MySQL 唯一约束，不会放宽额度。预占默认 5 分钟过期，
后台任务会自动释放未确认额度。AI/语音与用量服务默认只绑定 `127.0.0.1`；容器部署
时可改为内网地址，但不要将 8092/8093 直接映射到公网。

根目录单体已经提供可开关的用量服务客户端。启动 `usage-service` 后设置：

```env
TINGJIAN_USAGE_SERVICE_ENABLED=true
TINGJIAN_USAGE_SERVICE_URL=http://127.0.0.1:8093
TINGJIAN_USAGE_FAIL_OPEN=true
TINGJIAN_INTERNAL_AUTH_ENABLED=true
TINGJIAN_INTERNAL_SERVICE_TOKEN=请替换为至少32位随机值
```

公开的 `GET /api/v1/usage` 会把单体月度会话统计与用量服务的每日
`ASR_SECONDS`、`AI_REQUESTS`、`TTS_CHARACTERS` 合并返回。用量服务关闭或暂时不可用时，
接口自动退回本地统计，Android 仍可正常显示基础用量。

AI 请求按次预占；云端成功后确认，本地降级时释放。云端 ASR 按 30 秒块预占，
上传过音频的块会确认，空块会释放。默认关闭，因此不启动微服务时原有开发流程不受影响。

## 单 Agent 与流式回复

公开接口仍由单体提供：`POST /api/v1/agent/chat` 返回完整回复，
`POST /api/v1/agent/chat/stream` 返回 SSE 事件 `meta`、`delta`、`reset`、`done`。
单体校验 Bearer Token 和 `sessionId` 归属后，只向内部服务发送当前账号的数据。
第一版只读工具包括术语表、常用语、最近历史和用量；Agent 不能直接访问数据库，也不能
声称已经执行写操作。云端中断时会发送 `reset` 并切换本地回答，Android 可以清空不完整
片段后继续显示。

启用内部 AI 链路：

```env
TINGJIAN_AI_SPEECH_SERVICE_ENABLED=true
TINGJIAN_AI_SPEECH_SERVICE_URL=http://127.0.0.1:8092
TINGJIAN_AI_PROVIDER=remote
TINGJIAN_INSIGHT_PROVIDER=remote
TINGJIAN_AI_MODEL_PROVIDER=qwen
TINGJIAN_QWEN_API_KEY=replace-me
TINGJIAN_INTERNAL_SERVICE_TOKEN=请替换为至少32位随机值
```

不配置密钥时把 `TINGJIAN_AI_MODEL_PROVIDER` 保持为 `local`，服务仍可启动并返回安全降级结果。
