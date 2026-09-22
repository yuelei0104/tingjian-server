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

这一阶段保存的是**手动写入的文本消息**；实时语音识别、TTS、关键词高亮、正式用户登录与账号隔离将在后续开发。正式对外提供服务前必须把本地演示账号换成登录身份，并完成数据迁移和鉴权。
