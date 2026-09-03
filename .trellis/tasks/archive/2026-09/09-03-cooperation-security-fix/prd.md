# 修复cooperation-plugin SecurityConfig启动失败

## Goal
修复 `SecurityConfig` 中 `SecurityWebFilterChain` Bean 依赖 `ServerHttpSecurity` 在插件子上下文中无该 Bean 导致的 `UnsatisfiedDependencyException`，使插件在 Halo 2.20.11 上正常启动，合作表单 POST 保持匿名可访问，GET/DELETE/PUT 保持管理员鉴权。

## Background
- 1.1.1 修复 `Bean$Bootstrap` 重复加载后，启动进入下一阶段报错：`No qualifying bean of type ServerHttpSecurity`
- `SecurityConfig.cooperationSecurityFilterChain(ServerHttpSecurity http)` 试图注入 `ServerHttpSecurity` 原型 Bean，但该 Bean 仅在宿主 `app` 的 `ServerHttpSecurityConfiguration` 中，插件子上下文的 `PluginApplicationContext` 无法获取，`DefaultPluginApplicationContextFactory` 的父子隔离导致
- 宿主的 `SecurityWebFilterChain` 不应由插件子上下文提供，插件的匿名放行应由控制器层 `requireAdmin` 与 Halo 全局安全协同，或改为非 `/apis` 路径

## Requirements
- 删除 `config/SecurityConfig.java`（不再定义 `SecurityWebFilterChain` Bean）
- 保持 `CooperationController` 的 `RouterFunction` 注册路径 `/apis/api.cooperation.vivid.run/v1alpha1/cooperations` 不变，POST 在控制器层不鉴权直接处理，GET/DELETE/PUT 仍通过 `checkAdmin()` 区分 401/403
- 验证 Halo 全局安全对插件 `/apis/**` 路径的匿名放行：若删除后 POST 被宿主拦截为 401，则改用 `CustomEndpoint` 或将 POST 路径改为 `/api/cooperation` 并同步 `settings.yaml` 的 `endpoint`
- 版本 `1.1.1 → 1.1.2`（`build.gradle` 与 `plugin.yaml` 同步）
- 重新构建 `gradle :plugin-cooperation:build -x test` 成功，`jar tf` 含 `console/`，不含 `SecurityConfig.class`

## Acceptance Criteria
- [ ] 删除 `SecurityConfig.java` 后 `gradle build` 成功，`1.1.2` Jar 中不含该类
- [ ] 在 Halo 2.20.11 安装 `1.1.2` 后插件状态 `STARTED`，日志无 `UnsatisfiedDependencyException` / `ClassCastException`
- [ ] 匿名 `POST /apis/api.cooperation.vivid.run/v1alpha1/cooperations` 返回 200（或按校验返回 400），无需 Cookie
- [ ] 未登录 `GET /.../cooperations` 返回 401，已登录非管理员返回 403，管理员返回 200
- [ ] Console 菜单与 1.1.0 功能保持一致

## Notes
- 轻量修复，PRD-only
- 若删除后 POST 仍被拦截，需后续迭代将 POST 路径迁至 `/api/cooperation` 并更新主题 `endpoint`
