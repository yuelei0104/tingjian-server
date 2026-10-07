# Tingjian 端到端联调

本目录的 PowerShell 脚本用于验证以下调用链：

```text
Android / 测试脚本 -> gateway-service:8088 -> Tingjian-Server:8080
                                             -> usage-service:8093
                                             -> MySQL / Redis
```

## 1. 启动完整服务

根据 `.env.example` 设置环境变量后执行：

```powershell
.\scripts\Start-TingjianStack.ps1
```

脚本会构建并启动 Compose 服务，然后等待网关健康检查。已经构建过镜像时可使用：

```powershell
.\scripts\Start-TingjianStack.ps1 -SkipBuild
```

## 2. 运行冒烟测试

只检查网关健康状态和未登录拦截：

```powershell
.\scripts\Test-TingjianStack.ps1
```

使用已有测试账号执行完整链路：

```powershell
.\scripts\Test-TingjianStack.ps1 `
    -Email "test@example.com" `
    -Password "replace-with-test-password" `
    -RequireAuthenticated
```

也可以直接提供访问令牌：

```powershell
.\scripts\Test-TingjianStack.ps1 `
    -AccessToken $env:TINGJIAN_SMOKE_ACCESS_TOKEN `
    -RequireAuthenticated
```

完整测试会创建一段临时会话、写入一条消息、结束会话并删除测试历史。使用
`-SkipMutation` 可以只验证登录后的只读接口。

## 3. Android 地址

- Android 模拟器：`http://10.0.2.2:8088/`
- 同一局域网真机：`http://电脑IPv4地址:8088/`
- 发布环境：HTTPS 网关地址

Android 只访问网关，不应直接连接 `8080`、`8092` 或 `8093`。

## 4. 停止服务

```powershell
.\scripts\Stop-TingjianStack.ps1
```

只有确认不再需要本地数据时才执行：

```powershell
.\scripts\Stop-TingjianStack.ps1 -RemoveVolumes
```

该命令要求再次输入 `DELETE`，防止误删 MySQL 与 Redis 数据卷。
