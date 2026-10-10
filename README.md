# 经纬智能工具箱后端

这是独立的后端仓库，负责认证、会话、工具目录、策略、额度、幂等、审计和具体工具执行器的统一编排；不包含 HarmonyOS 客户端或 Vue 管理后台，也不伪造尚未接入的工具结果。

## 技术栈

- Java 21
- Spring Boot 4.1.1 / Spring MVC / Spring Security
- Spring JDBC + MySQL 8
- Flyway 数据库迁移
- Redis 7（令牌撤销、游客工具窗口额度）
- Maven Wrapper

## 已实现的平台能力

- 公开注册、登录、游客会话、刷新令牌、注销和 `/api/v1/me`
- HMAC-SHA256 JWT 访问令牌（默认 15 分钟）和数据库轮换刷新令牌（默认 30 天）
- ADMIN 角色保护的工具目录、工具策略和使用量接口
- 每个工具独立的启用状态、是否必须登录、游客额度和窗口；`guestLimit=null` 为不限额
- Redis Lua 原子额度扣减；Redis 不可用时相关请求 fail-closed
- MySQL 幂等记录、使用量、审计日志和乐观锁版本冲突
- `ToolExecutor` 扩展点。没有具体执行器的工具返回 `501 TOOL_NOT_IMPLEMENTED`，不会消耗额度
- 统一响应、错误码、`X-Request-Id`、Flyway `V1__platform_schema.sql`

## 本地启动

1. 准备 MySQL 8 和 Redis 7。可以使用仓库内的 `compose.yaml`：

   ```powershell
   Copy-Item .env.example .env
   docker compose up -d mysql redis
   ```

2. 将 `.env` 导入当前 PowerShell（Spring Boot 不会自动读取 `.env`）：

   ```powershell
   Get-Content .\.env | ForEach-Object {
     if ($_ -match '^\s*([^#=]+?)\s*=(.*)$') {
       [Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim(), 'Process')
     }
   }
   ```

3. 启动：

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   `FLYWAY_ENABLED=true` 时启动会自动执行 `src/main/resources/db/migration`。必须配置至少 32 字节的 `JWT_SECRET`。

4. 第一次部署需要创建管理员（只在不存在同名账号时创建）：

   ```text
   BOOTSTRAP_ADMIN_ENABLED=true
   BOOTSTRAP_ADMIN_IDENTIFIER=admin@example.com
   BOOTSTRAP_ADMIN_PASSWORD=<通过部署密钥注入，至少 8 位>
   ```

## 接口和目录

- 机器可读接口契约：`docs/api/openapi.json`
- 字段、错误码和流程说明：`docs/API_CONTRACT.md`
- `src/main/java/.../auth`：注册、认证、会话
- `src/main/java/.../tool`：工具元数据、策略、额度、幂等、执行扩展点
- `src/main/java/.../common`：响应、异常、请求追踪、JWT
- `src/main/resources/db/migration`：Flyway 迁移

## 验证

```powershell
.\mvnw.cmd -B test
python .\scripts\verify-contract.py
```

容器和实际 MySQL/Redis 业务验收需要本机 Docker 或可用的 MySQL 8/Redis 7 实例；没有数据库凭据时不会绕过认证，也不会使用内存数据库冒充生产验收。
