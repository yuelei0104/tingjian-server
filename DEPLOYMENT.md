# 听见后端运行与部署

## 本地直接运行

MySQL 和 Redis 使用当前开发端口 `13306`、`16379`。在 PowerShell 中设置数据库密码后运行：

```powershell
$env:TINGJIAN_DB_PASSWORD="你的数据库密码"
./mvnw.cmd spring-boot:run
```

健康检查地址为 `http://127.0.0.1:8080/actuator/health`。每个 HTTP 响应都会返回
`X-Request-Id`，后端日志使用相同编号，排查错误时不需要记录访问令牌或请求正文。

## Docker Compose

复制 `.env.example` 为 `.env`，填写强密码、验证码 pepper 和通知网关地址，然后执行：

```powershell
docker compose up -d --build
docker compose ps
```

Compose 会同时启动单体业务服务、用量服务、AI/语音服务和 8088 网关。对外联调统一使用
`http://127.0.0.1:8088`；`8092`、`8093` 只在容器网络中开放。可以通过
`http://127.0.0.1:8088/gateway/status` 和
`http://127.0.0.1:8088/actuator/health` 检查网关。

如果电脑上已有 `tingjian-mysql` 或 `tingjian-redis` 容器，请先继续使用现有开发方式，
不要直接启动同名 Compose 服务。确认数据已经备份后再安排迁移。

## 数据库迁移

Flyway 使用 `db/migration/V1__baseline.sql`。新数据库会创建完整结构；已有数据库会先在
版本 `0` 建立基线，然后执行幂等的 V1 脚本，只补充缺少的表，不删除已有数据。

## 生产要求

- 使用 `prod` profile，所有密码和令牌只通过环境变量传入；
- 在反向代理或网关终止 HTTPS，不直接公开 MySQL 和 Redis；
- `TINGJIAN_CORS_ALLOWED_ORIGINS` 只填写真实前端域名，多个域名使用英文逗号分隔；
- Actuator 仅公开 `health` 和 `info`，健康详情不会返回数据库凭据；
- 日志不记录 Authorization、验证码、密码或请求正文。
## 云端 AI 与语音

云服务默认关闭，因此缺少密钥不会影响应用启动。单体直连模式仍可使用
`DASHSCOPE_API_KEY` 与 `TINGJIAN_AI_PROVIDER=qwen`。推荐部署方式是让单体只调用内部
AI 服务：

```env
TINGJIAN_AI_SPEECH_SERVICE_ENABLED=true
TINGJIAN_AI_SPEECH_SERVICE_URL=http://127.0.0.1:8092
TINGJIAN_AI_PROVIDER=remote
TINGJIAN_INSIGHT_PROVIDER=remote
TINGJIAN_AI_MODEL_PROVIDER=qwen
TINGJIAN_QWEN_API_KEY=replace-me
TINGJIAN_ASR_PROVIDER=aliyun
```

生产环境建议配置华北 2（北京）的业务空间专属地址，不要把密钥或业务空间 ID
提交到源码。Android 会保留系统语音识别和系统 TTS 作为自动降级方案。
`TINGJIAN_AI_MODEL_PROVIDER=local` 可在没有密钥时验证完整服务链路；本地降级不会消耗
AI 云端额度。公开 Agent SSE 接口位于 `/api/v1/agent/chat/stream`，反向代理需要关闭
响应缓冲并将读取超时设置为至少 60 秒。

## 微服务用量闭环

先启动 `microservices/usage-service`，确认
`http://127.0.0.1:8093/actuator/health` 正常，再启用单体到用量服务的调用：

```env
TINGJIAN_USAGE_SERVICE_ENABLED=true
TINGJIAN_USAGE_SERVICE_URL=http://127.0.0.1:8093
TINGJIAN_USAGE_FAIL_OPEN=true
TINGJIAN_INTERNAL_AUTH_ENABLED=true
TINGJIAN_INTERNAL_SERVICE_TOKEN=请替换为至少32位随机值
```

AI 建议按成功的云端请求计量；本地模板或云端降级不会消耗 AI 额度。云端 ASR
按 30 秒块预占，开始上传音频后确认，未上传音频则释放。迁移期默认 `fail-open=true`，
用量服务短暂不可用时主业务仍可继续；生产监控与持久化完成后可切换为 `false`。

用量服务以 MySQL 为权威数据源，Redis 只保存可丢失的幂等索引缓存。预占记录默认
5 分钟后过期并自动返还额度；服务重启后套餐、已用额度、预占状态和幂等结果不会丢失。
用量服务使用独立的 `usage_flyway_schema_history`，可以与单体共享数据库实例而不冲突。

`TINGJIAN_INTERNAL_SERVICE_TOKEN` 必须在单体、`usage-service` 和 `ai-speech-service`
中保持一致，不要提交真实值。网关不转发 `/internal/**`，并会移除外部请求中伪造的
内部令牌。服务默认绑定 `127.0.0.1`；容器部署时再通过
`TINGJIAN_USAGE_BIND_ADDRESS=0.0.0.0` 和
`TINGJIAN_AI_SPEECH_BIND_ADDRESS=0.0.0.0` 在容器网络内开放。

## 本地端到端验收

Windows 环境可使用 `scripts/Start-TingjianStack.ps1` 启动完整 Compose 链路，随后运行
`scripts/Test-TingjianStack.ps1`。脚本会检查网关健康状态、未登录拦截，并可使用已有
测试账号验证首页、用量、会话、消息、结束会话与清理流程。详细命令见
`SMOKE-TEST.md`。
