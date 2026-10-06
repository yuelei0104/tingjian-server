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

绑定手机号后，也可以通过短信找回密码：

1. 登录后调用 `POST /api/v1/account/phone/verification` 请求绑定验证码；
2. 调用 `PUT /api/v1/account/phone` 完成绑定或换绑；
3. 忘记密码时调用 `POST /api/auth/password/forgot/sms`；
4. 最后调用 `POST /api/auth/password/reset/sms` 设置新密码。

手机号统一使用 E.164 格式，例如 `+8613800138000`。解绑手机号需要再次提交当前密码。

## 登录设备与安全提醒

Android 客户端会随注册、登录请求发送安装级设备标识、设备名称、系统版本和应用版本。
服务端只保存设备标识的哈希，并记录会话 IP 与最近活动时间。

- `GET /api/v1/account/sessions`：查看全部有效会话并标记当前设备；
- `DELETE /api/v1/account/sessions/{sessionId}`：撤销指定设备；
- `DELETE /api/v1/account/sessions`：退出除当前设备以外的全部会话。

已注册账号从新设备登录时，会通过安全通知网关发送登录提醒。通知失败不会阻断正常登录。

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
邮箱验证码使用 `EMAIL`，手机号绑定和短信找回使用 `SMS`。新设备登录提醒的
`purpose` 为 `NEW_LOGIN`，同时包含设备、平台、IP 和登录时间。网关可以在内部再对接
阿里云短信、腾讯云短信或邮件供应商；不要把云服务密钥提交到 Git。
