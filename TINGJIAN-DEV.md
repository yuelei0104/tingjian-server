# 听见后端：会话接口（第一阶段）

## 先处理已进入 Git 历史的数据库密码

最初提交的 `application.properties` 含数据库明文密码。即使当前仓库是私有的，该值仍保留在 Git 历史里。先在 IDEA 中将 `spring.datasource.password` 改为 `${TINGJIAN_DB_PASSWORD}`；再在 MySQL 中分别修改 `tingjian` 应用账号和 `root` 账号为不同的新密码，并更新 IDEA 运行配置中的环境变量。不要把新密码放进代码、截图或 Git 提交。已经暴露的旧密码必须作废。不要删卷或重建数据库。

在命令行运行 `docker exec -it tingjian-mysql mysql -u root -p`，输入当前 root 密码后，在 `mysql>` 提示符里逐条执行（自己替换尖括号中的值，保留单引号）：

```sql
ALTER USER 'tingjian'@'%' IDENTIFIED BY '<新的应用账号密码>';
ALTER USER 'root'@'%' IDENTIFIED BY '<新的root密码>';
ALTER USER 'root'@'localhost' IDENTIFIED BY '<新的root密码>';
exit
```

如果修改应用账号后 IDEA 尚未更新 `TINGJIAN_DB_PASSWORD`，重启应用会暂时无法连接数据库；更新环境变量后恢复。Docker 容器初建时的 `MYSQL_*_PASSWORD` 环境值不随 SQL 改密自动更新，后续以数据库里的新密码为准。

## 运行

在 IDEA 的 `TingjianServerApplication` 运行配置中设置环境变量 `TINGJIAN_DB_PASSWORD=新的应用账号密码`，将“有效配置文件”（Active profiles）设为 `dev`，重启程序。开发模式绑定 `127.0.0.1:8080`，启动时用 `db/schema.sql` 中的 `CREATE TABLE IF NOT EXISTS` 建表。它不会删除原有表或数据。首次启动前先确认 MySQL 容器运行。`dev` 接口没有正式登录，固定使用本机演示账号；请勿将 `dev` 模式部署到公网。

IDEA Maven 面板运行 `test` 可执行单元测试和 Spring Boot 上下文测试；测试使用内存 H2，不依赖本机 MySQL。也可以在命令窗口运行 `mvnw.cmd test`。

登录后的客户端设置使用 `GET /api/v1/preferences` 读取，使用
`PUT /api/v1/preferences` 整体更新。首次读取返回 `configured=false`，客户端可将本机设置作为初始值上传。

登录后的“用量与额度”页面使用 `GET /api/v1/usage`。接口按 UTC 自然月统计
已结束会话时长、云端文字字符数和会话数，并返回 V1 内测额度、剩余额度和进度。
当前套餐不可购买，不会产生自动扣费；未同步到服务端的本机内容不计入统计。

## 手动调用（Windows CMD）

以下命令逐条执行。创建会话后，从统一响应的 `data.id` 复制会话 ID，把后续命令里的 `你的会话ID` 替换成它。所有时间字段按 UTC 存储和返回。

```bat
curl.exe -X POST http://127.0.0.1:8080/api/dev/sessions -H "Content-Type: application/json" -d "{\"title\":\"Demo\"}"
curl.exe -X POST http://127.0.0.1:8080/api/dev/sessions/你的会话ID/messages -H "Content-Type: application/json" -d "{\"speaker\":\"OTHER\",\"content\":\"Hello\"}"
curl.exe -X POST http://127.0.0.1:8080/api/dev/sessions/你的会话ID/messages -H "Content-Type: application/json" -d "{\"speaker\":\"SELF\",\"content\":\"Thanks\"}"
curl.exe http://127.0.0.1:8080/api/dev/sessions
curl.exe http://127.0.0.1:8080/api/dev/sessions/你的会话ID
curl.exe -X POST http://127.0.0.1:8080/api/dev/sessions/你的会话ID/end
curl.exe "http://127.0.0.1:8080/api/dev/history?keyword=Hello&page=0&size=20"
curl.exe http://127.0.0.1:8080/api/dev/history/你的会话ID
curl.exe -X DELETE http://127.0.0.1:8080/api/dev/history/你的会话ID
```

所有接口使用 `{requestId, code, message, data, timestamp}` 统一响应。列表支持 `?page=0&size=20`（每页最大 50）；历史接口还支持用 `keyword` 搜索标题和对话内容。消息角色 `OTHER` 表示对方，`SELF` 表示我。结束后重复结束仍会返回会话；结束后追加消息返回 HTTP 409，不存在的会话返回 HTTP 404。标题最多 80 字符，单条文本最多 2000 字符。

## 关键词、术语库和常用语

以下三个资源均支持 `GET` 列表、`POST` 新增、`PUT /{id}` 修改和 `DELETE /{id}` 删除。新增后从响应的 `data.id` 获取资源 ID。

```bat
curl.exe -X POST http://127.0.0.1:8080/api/dev/keywords -H "Content-Type: application/json" -d "{\"phrase\":\"老师\",\"vibrationEnabled\":true,\"priority\":80,\"enabled\":true}"
curl.exe http://127.0.0.1:8080/api/dev/keywords

curl.exe -X POST http://127.0.0.1:8080/api/dev/glossary -H "Content-Type: application/json" -d "{\"term\":\"人工智能\",\"alias\":\"AI\",\"language\":\"zh-CN\",\"category\":\"课堂\",\"priority\":90,\"enabled\":true}"
curl.exe http://127.0.0.1:8080/api/dev/glossary

curl.exe -X POST http://127.0.0.1:8080/api/dev/quick-phrases -H "Content-Type: application/json" -d "{\"content\":\"请再说一次\",\"category\":\"日常\",\"sortOrder\":10,\"enabled\":true}"
curl.exe http://127.0.0.1:8080/api/dev/quick-phrases
```

## 实时文字会话与断线恢复

登录后可使用 `ws://127.0.0.1:8080/ws/realtime`。握手请求必须携带
`Authorization: Bearer <accessToken>`。客户端发送：

```json
{
  "type": "MESSAGE",
  "sessionId": "会话 ID",
  "clientMessageId": "客户端生成且在该会话内唯一的 ID",
  "speaker": "OTHER",
  "content": "你好"
}
```

服务端保存成功后向发送端返回 `ACK`，并向该账号的其他在线连接广播
`MESSAGE`。相同 `clientMessageId` 和相同内容可安全重试；相同编号对应不同内容时
返回 `IDEMPOTENCY_CONFLICT`。每条消息带递增的 `sequence`。

断线恢复使用
`GET /api/v1/sessions/{id}/messages?afterSequence=0&size=100`，按响应中的
`nextAfterSequence` 继续读取，直到 `hasNext=false`。Android 客户端先将待发消息
写入本机持久化队列，收到 `ACK` 后才移除；WebSocket 断开时自动退避重连，最终
仍通过 REST 幂等补齐并结束会话。因此 WebSocket 用于低延迟，REST 用于恢复，
不能把“已调用 send”当作已经保存成功。

实时语音识别和 TTS 仍使用 Android 系统能力，第三方云 ASR/TTS 暂未接入。
