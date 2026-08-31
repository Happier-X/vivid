# 自建合作表单 Halo 插件后端

## 目标

在自有服务器与 Halo 同机部署的前提下，为 `page_cooperation.html` 原生表单提供自建 Halo 插件后端，替代此前的 Cloudflare Worker 参考实现，提供同源提交接口、服务端校验、限流、邮件通知与数据留存能力，彻底摆脱外部依赖。

## 背景与约束

- 前置：`08-31-cooperation-form-native` 已将表单改为原生 `fetch POST JSON` 到 `theme.config.cooperation.endpoint`，`worker/cooperation.ts` 仅为参考
- 用户选择方案 A（Halo 插件），要求插件 Jar 部署于现有 Halo 服务器的 `plugins` 目录，与 Halo 同进程、同域，无跨域
- Halo 版本 `>=2.20`，插件基于 `run.halo.app.plugin.BasePlugin` + Spring Boot 3.x + RouterFunction
- 主题保持纯静态，插件为独立构建产物 `plugin-cooperation/`

## 需求

### R1 插件工程

- 目录 `plugin-cooperation/`：`build.gradle`（`run.halo.tools.plugin:halo-plugin`）、`src/main/java/run/wanchun/cooperation/`、`src/main/resources/plugin.yaml`
- `plugin.yaml` 声明：`name: cooperation-plugin`、`version: 1.0.0`、`requires: ">=2.20.0"`，提供 `setting` 表单引用
- 一键构建 `./gradlew build` 产出 `build/libs/cooperation-plugin-1.0.0.jar`，`README.md` 说明拷贝到 Halo `plugins/` 并重启

### R2 提交接口（匿名可访问）

- `POST /apis/api.cooperation.vivid.run/v1alpha1/cooperations`（插件路由），`Content-Type: application/json`
- 请求体与前端契约一致：
  ```json
  {
    "company": "",
    "contact": "",
    "phone": "",
    "type": "institution|community|home_government|channel_oem",
    "typeLabel": "",
    "message": "",
    "website": "",
    "sourceUrl": "",
    "userAgent": "",
    "timestamp": ""
  }
  ```
- 响应统一：`{ "success": boolean, "message": string }`，状态码 `200/400/429/500`
- 校验失败 → `400`，限流命中 → `429`，邮件失败 → `500`，蜜罐有值 → `200` 静默成功

### R3 服务端校验（与前端一致）

- `company: 必填 2-50`，`contact: 必填 2-20`，`phone: 必填 正则 ^(1[3-9]\d{9}|0\d{2,3}-?\d{7,8})$`，`type: 4枚举必填`，`message: 0-500`，`website: 必须为空`
- 使用 Jakarta Validation + 手写校验器，错误信息与前端文案语义对齐

### R4 限流与风控

- IP 维度内存限流默认 `60s 1次`，可通过插件设置 `rateLimitSeconds` 配置
- 实现 `ConcurrentHashMap<String, Long>` + `synchronized` 清理过期，支持多实例下退化为单机限流（文档说明如需分布式请接 Redis）
- 蜜罐 `website` 非空直接返回成功不落库、不发邮件

### R5 邮件通知

- 通过 `JavaMailSender`（Spring Mail）发送，配置来自插件设置：`smtpHost/port/username/password/from/receiverEmail`，支持 SSL/TLS
- 标题 `【万椿官网】合作咨询 - {company} - {typeLabel}`，正文含全部字段 + `sourceUrl/userAgent/timestamp`
- 发送失败向日志输出并返回 `500`，供前端展示“邮件发送失败”

### R6 数据留存（Halo Extension）

- 定义 CRD：`group: api.cooperation.vivid.run`、`version: v1alpha1`、`kind: Cooperation`、`singular: cooperation`、`plural: cooperations`
- Spec 字段：`company/contact/phone/type/typeLabel/message/sourceUrl/userAgent/timestamp/ip`
- 通过 `ReactiveExtensionClient` 创建 Extension，持久化到 Halo 数据库，支持后续查询

### R7 查询接口（需鉴权）

- `GET /apis/api.cooperation.vivid.run/v1alpha1/cooperations?page=0&size=20` 需 `ROLE_ADMIN`，返回 `ListResult<Cooperation>` 分页
- `GET /apis/api.cooperation.vivid.run/v1alpha1/cooperations/{name}` 详情
- `DELETE` 预留（可选，首期可不做）

### R8 插件设置

- `src/main/resources/plugin.yaml` 中 `spec.settingName` 指向 `cooperation-setting`，`settings.yaml` 定义分组：
  - `smtpHost` `smtpPort` `smtpUsername` `smtpPassword` `fromEmail` `receiverEmail` `rateLimitSeconds`
- Halo 控制台 → 插件 → 合作插件 → 设置 可视化配置，无需改配置文件重启（`ConfigMap` 热更新）

### R9 主题联动与部署

- `settings.yaml`（主题）中 `cooperation.endpoint` 默认值改为 `/apis/api.cooperation.vivid.run/v1alpha1/cooperations`
- 部署后前端同源请求，无 CORS
- 提供 `plugin-cooperation/README.md`：构建、安装、配置 SMTP、前端联调、排错

## 非目标

- 插件控制台前端 UI（首期仅 API，查看可通过 Halo `curl /apis/...` 或直接查 Extension）
- 短信验证码、附件、富文本、工作流审批
- 分布式限流（Redis）

## 验收标准

- [ ] 1. `./gradlew :plugin-cooperation:build` 在本地生成 Jar，`plugin.yaml` 可被 Halo 识别
- [ ] 2. 安装插件后，`POST /apis/api.cooperation.vivid.run/v1alpha1/cooperations` 匿名可访问，`GET` 需登录鉴权
- [ ] 3. 非法 payload（空 company/非法 phone/超长 message/蜜罐有值）按 R2 返回对应状态码与 message
- [ ] 4. 同一 IP 60s 内二次提交返回 `429`
- [ ] 5. 提交成功后可在 Halo 侧通过 `ReactiveExtensionClient` 查到 `Cooperation` 记录，且收到邮件（配置 SMTP 后）
- [ ] 6. 主题 `settings.yaml` 默认 `endpoint` 指向插件同源路径，前端提交不再依赖 `worker/`，`pnpm build` 通过
- [ ] 7. 提供 `plugin-cooperation/README.md` 与 `plugin-cooperation/docs/DEPLOY.md`，含构建、安装、SMTP 配置、前端联调
- [ ] 8. `pnpm check` 与插件 `./gradlew check` 均通过，无新增 lint 报错

## 约束

- 遵循 `.trellis/spec/frontend/halo-theme.md`：主题侧 `settings.yaml` 与 `theme.config` 契约双向同步
- 插件遵循 Halo 插件规范（`plugin.yaml` + `BasePlugin`），不直接修改 Halo 核心
- 不引入飞书相关代码与文档
