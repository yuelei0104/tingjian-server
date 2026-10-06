# Tingjian 渐进式微服务架构

这一目录提供与现有单体后端并行的微服务运行面。根目录中的 Spring Boot 应用仍然是当前稳定业务实现，迁移期间不删除、不改端口，也不要求 Android 立即切换接口。

## 第一阶段服务

| 服务 | 默认端口 | 当前职责 |
| --- | ---: | --- |
| `gateway-service` | 8088 | 统一入口、HTTP/WebSocket 路由、请求链路 ID |
| `ai-speech-service` | 8092 | AI、ASR、TTS 提供商配置与能力边界 |
| `usage-service` | 8093 | 套餐、日配额、用量预占/确认/释放和幂等处理 |
| 根目录单体应用 | 8080 | 账号、会话、历史、个性化等现有稳定业务 |

网关会把 `/api/**` 和 `/ws/**` 转发给现有单体，把 `/internal/ai/**` 与 `/internal/usage/**` 转发给新服务。因此 Android 后续只需要把 API 基地址改为网关地址，不需要一次性修改所有接口。

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
| `TINGJIAN_AI_SPEECH_URL` | `http://127.0.0.1:8092` |
| `TINGJIAN_USAGE_URL` | `http://127.0.0.1:8093` |

## 迁移顺序

1. Android 真机只切换到网关 8088，验证旧接口和 WebSocket 都可透传。
2. 在调用云端 ASR/AI 前向 `usage-service` 预占用量，成功后确认，失败时释放。
3. 将现有 AI/语音实现从单体迁入 `ai-speech-service`，对 Android 保持原有外部契约。
4. 再拆账号与会话服务；到这一步才分别迁移数据库表和引入可靠事件/消息队列。

> `usage-service` 当前使用进程内存储，只用于验证接口与业务规则。生产部署前应替换为 MySQL/Redis，并限制 `/internal/**` 只能由网关或服务网络访问。

根目录单体已经提供可开关的用量服务客户端。启动 `usage-service` 后设置：

```env
TINGJIAN_USAGE_SERVICE_ENABLED=true
TINGJIAN_USAGE_SERVICE_URL=http://127.0.0.1:8093
TINGJIAN_USAGE_FAIL_OPEN=true
```

AI 请求按次预占；云端成功后确认，本地降级时释放。云端 ASR 按 30 秒块预占，
上传过音频的块会确认，空块会释放。默认关闭，因此不启动微服务时原有开发流程不受影响。
