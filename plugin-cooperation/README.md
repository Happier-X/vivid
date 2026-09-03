# 合作咨询插件（cooperation-plugin）1.1.0

自建 Halo 插件后端，为 `page_cooperation.html` 原生表单提供同源提交、Extension 持久化与 **Console 后台管理**，替代 Cloudflare Worker / FastAPI 参考实现。

> 自 1.1.0 起，`server/cooperation`（FastAPI）与 `worker/cooperation.ts`（Cloudflare Worker）已 **废弃 Deprecated**，主题 `settings.yaml` 的 `cooperation.endpoint` 已固定为插件同源路径 `/apis/api.cooperation.vivid.run/v1alpha1/cooperations`，不再维护 `API_KEY`/SQLite 方案。

## 功能（1.1.0 新增后台管理）

- `POST /apis/api.cooperation.vivid.run/v1alpha1/cooperations` 匿名提交
- 服务端校验（公司 2-50、联系人 2-20、手机号正则、枚举、message 0-500、蜜罐）
- IP 内存限流（默认 60s 1 次，可配 `rateLimitSeconds`）
- 蜜罐 `website` 非空静默成功，不落库不发邮件
- 通过 `ReactiveExtensionClient` 创建 `Cooperation` Extension 持久化（`spec.handled` 已处理标记，默认 false，历史数据兼容）
- 通过 `JavaMailSender` 发送通知邮件（Halo 控制台 → 插件设置配置 SMTP）
- `GET /apis/api.cooperation.vivid.run/v1alpha1/cooperations` 需 `ROLE_ADMIN`，支持分页 `page/size`（10/20/50）、筛选 `keyword`（company/contact/phone 模糊）、`type` 单选、`handled`（all/true/false）、时间区间 `startTime/endTime`，详情 `GET /{name}`
- `DELETE /apis/.../cooperations/{name}` 需 `ROLE_ADMIN`，单条删除需二次确认
- `PUT /apis/.../cooperations/{name}/handled` 需 `ROLE_ADMIN`，切换已处理/未处理
- `GET /apis/.../cooperations/export` 需 `ROLE_ADMIN`，导出当前筛选结果为 CSV（UTF-8 BOM），文件名 `cooperations-YYYYMMDD-HHmmss.csv`
- **Console 前端**：Halo Console 左侧菜单“合作咨询”→ 列表/分页/搜索/筛选/详情抽屉/标记已处理/删除二次确认/导出/空状态/Toast，401 跳登录、403 提示无权限

## 目录结构

```
plugin-cooperation/
├── build.gradle
├── console/                         # Console 前端（Vue3 + Vite）
│   ├── src/index.ts                 # definePlugin 注册路由与菜单
│   ├── src/views/CooperationList.vue
│   └── src/api/cooperation.ts
├── src/main/java/run/wanchun/cooperation/
│   ├── CooperationPlugin.java
│   ├── config/CooperationProperties.java
│   ├── config/SecurityConfig.java
│   ├── extension/Cooperation.java   # 新增 spec.handled
│   ├── dto/CooperationRequest.java
│   ├── dto/CooperationResponse.java
│   ├── service/RateLimiter.java
│   ├── service/EmailService.java
│   └── controller/CooperationController.java  # 新增筛选/删除/handled/export
├── src/main/resources/
│   ├── plugin.yaml                  # 1.1.0 + console 入口
│   ├── console/main.js + style.css  # Vite 产物（随 Jar 打入）
│   └── extensions/setting.yaml
└── docs/DEPLOY.md
```

## 构建

要求 JDK 17+、Gradle 8.x、Node 18+、pnpm 10+

```bash
# 1. 构建 Console（输出到 src/main/resources/console）
cd plugin-cooperation/console && pnpm install && pnpm build

# 2. 构建插件（产物含 console 入口）
./gradlew :plugin-cooperation:build -x test
# 产物
plugin-cooperation/build/libs/cooperation-plugin-1.1.0.jar
jar tf plugin-cooperation/build/libs/cooperation-plugin-1.1.0.jar | grep console
# console/main.js
# console/style.css
```

Halo 2.20+ 兼容，基于 `run.halo.app.plugin.BasePlugin` + Spring Boot 3.x + RouterFunction。

## 安装

1. 拷贝 Jar 到 Halo 的 `plugins/` 目录：
   ```bash
   cp plugin-cooperation/build/libs/cooperation-plugin-1.1.0.jar /path/to/halo/plugins/
   ```
2. 重启 Halo（或等待自动热加载）
3. Halo 控制台 → 插件 → 合作咨询插件 → 启用 → 设置
4. 左侧菜单出现“合作咨询”，点击即可查看列表

## 插件设置（Halo 控制台 → 插件设置）

- `smtpHost` / `smtpPort` / `smtpUsername` / `smtpPassword` / `fromEmail` / `receiverEmail` / `smtpSsl` / `rateLimitSeconds`
- 修改后无需重启，配置通过 `CooperationProperties`（`plugin.cooperation.*`）注入
- 未配置 SMTP 时，仍落库但跳过发信并输出 `WARN` 日志

## 主题联动

- 主题 `settings.yaml` 中 `cooperation.endpoint` 默认值已改为同源路径：
  `/apis/api.cooperation.vivid.run/v1alpha1/cooperations`
- 前端 `src/js/cooperation-form.ts` 支持相对路径（`/apis/...`）与绝对 `https://`，同源请求无 CORS
- 若插件未启用，前端提交将返回 404，前端展示“提交失败，请稍后重试或电话联系”

## Console 使用

- 左侧“合作咨询”入口（需管理员登录）
- 顶部：关键词搜索 + 合作类型单选 + 处理状态单选（全部/已处理/未处理）+ 时间区间 + 搜索/重置 + 导出 CSV
- 表格：公司/联系人/电话/合作类型（含中文标签）/意向截断/提交时间/ IP /来源页面/状态标签 + 操作（查看/标记已处理/删除）
- 分页：10/20/50，切换自动请求
- 详情抽屉：展示 `company/contact/phone/type/typeLabel/message/sourceUrl/userAgent/timestamp/ip/creationTimestamp/handled` 全量，支持切换状态
- 删除：二次确认，成功后自动刷新
- 导出：导出当前筛选结果，Excel 中文不乱码（含 BOM）

## 联调示例

```bash
# 成功提交
curl -X POST http://localhost:8090/apis/api.cooperation.vivid.run/v1alpha1/cooperations \
  -H "Content-Type: application/json" \
  -d '{"company":"山东测试公司","contact":"张三","phone":"13812345678","type":"institution","typeLabel":"养老机构合作","message":"合作意向","website":"","sourceUrl":"https://example.com/cooperation","userAgent":"curl","timestamp":"2026-08-31T00:00:00Z"}'

# 非法 phone -> 400
curl -X POST ... -d '{"company":"AB","contact":"张三","phone":"123","type":"institution"}'

# 蜜罐 -> 200 静默成功
curl -X POST ... -d '{"company":"AB","contact":"张三","phone":"13812345678","type":"institution","website":"spam"}'

# 限流 -> 429（60s 内二次提交）

# 查询（需管理员 Cookie/Token，支持筛选）
curl "http://localhost:8090/apis/api.cooperation.vivid.run/v1alpha1/cooperations?page=0&size=20&keyword=测试&type=institution&handled=false" \
  -H "Cookie: halo-SESSION=xxx"

# 标记已处理
curl -X PUT http://localhost:8090/apis/api.cooperation.vivid.run/v1alpha1/cooperations/{name}/handled \
  -H "Content-Type: application/json" -H "Cookie: halo-SESSION=xxx" \
  -d '{"handled":true}'

# 删除
curl -X DELETE http://localhost:8090/apis/api.cooperation.vivid.run/v1alpha1/cooperations/{name} -H "Cookie: halo-SESSION=xxx"

# 导出 CSV
curl "http://localhost:8090/apis/api.cooperation.vivid.run/v1alpha1/cooperations/export?keyword=测试" -H "Cookie: halo-SESSION=xxx" -o cooperations.csv

# 未登录 -> 401， 非管理员 -> 403
```

## 排错

- `400` 检查服务端校验文案是否与前端一致
- `401` 未登录，跳转 `/login`
- `403` 非管理员，提示无权限
- `429` 等待 `rateLimitSeconds` 后重试，或在插件设置调小
- `500` 查看 Halo 日志 `CooperationController` / `EmailService`，常见为 SMTP 配置错误
- 限流为内存 `ConcurrentHashMap`，多实例部署退化为单机限流，如需分布式请接入 Redis（文档说明）
- Console 菜单不显示：检查 `jar tf ... | grep console` 是否含 `console/main.js`，并确认 Halo 版本 >=2.20.0

## 合规

- 无飞书相关代码与文档
- 插件遵循 Halo 插件规范（`plugin.yaml` + `BasePlugin`），不修改 Halo 核心
- 依赖 `run.halo.app:api:2.20.11`，兼容 `>=2.20.0`
