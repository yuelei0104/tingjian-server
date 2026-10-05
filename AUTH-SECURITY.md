# 听见认证安全配置

## 接口流程

注册前调用 `POST /api/auth/email-verification/request`，请求体：

```json
{"email":"user@example.com"}
```

响应中的 `verificationId` 和收到的 6 位验证码需要随注册请求提交。验证码 10 分钟
过期、最多校验 5 次，数据库只保存带服务端 pepper 的 HMAC。

忘记密码先调用 `POST /api/auth/password/forgot`，再调用
`POST /api/auth/password/reset`：

```json
{
  "email": "user@example.com",
  "verificationId": "挑战 ID",
  "verificationCode": "123456",
  "newPassword": "new-password"
}
```

重置成功后，该账号的全部旧登录会话都会失效。

## 限流

- 同一邮箱 15 分钟最多登录失败 5 次。
- 同一来源 IP 15 分钟最多登录失败 25 次。
- 同一验证码目标 60 秒内只允许发送一次。
- 同一来源 IP 每天最多请求 30 次验证码。

限流状态存储在 Redis，适用于后续多实例部署。

## 开发与生产发送方式

开发模式 `tingjian.auth.verification-delivery=log` 会把验证码写到后端日志。

生产环境使用 `prod` profile，并设置：

```text
TINGJIAN_VERIFICATION_PEPPER=<随机长密钥>
TINGJIAN_VERIFICATION_WEBHOOK_URL=<邮件或短信网关地址>
TINGJIAN_VERIFICATION_WEBHOOK_TOKEN=<网关鉴权令牌>
```

Webhook 请求包含 `channel`、`destination`、`purpose`、`code` 和 `validMinutes`。
邮件服务可以直接接入；短信服务还需要先增加手机号绑定和验证流程，再将 `channel`
设置为 `SMS`。不要把云服务密钥提交到 Git。
