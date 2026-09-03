# Halo合作咨询插件后台管理页

## Goal

为 `cooperation-plugin` 新增 Halo Console 后台管理页，让运营/管理员在 Halo 管理后台直接查看、筛选、分页、详情、删除与导出合作表单提交数据，体验对标官方表单插件，无需额外 FastAPI 容器或外部接口，数据与 Halo 账号体系打通。

## Background

- 已有 `plugin-cooperation` 提供 `POST /apis/api.cooperation.vivid.run/v1alpha1/cooperations` 匿名提交、内存限流、蜜罐、Pydantic 校验、Extension 持久化（`Cooperation`）、邮件通知，`GET /apis/.../cooperations` 需 `ROLE_ADMIN` 鉴权
- 当前 `GET` 仅能通过 `curl` + Cookie 查询，无任何可视化列表页，运营无法在后台查看
- 用户明确倾向“只用 Jar 插件，像官方表单插件一样”，FastAPI 视为可废弃备选，主题 `settings.yaml` 中 `cooperation.endpoint` 已指向插件同源路径
- Halo >=2.20.0，插件基于 `BasePlugin` + Spring Boot 3.2.5 + RouterFunction，无前端代码

## Requirements

### 1. 列表查看

- 在 Halo Console 左侧新增一级或插件子菜单“合作咨询”
- 表格列：公司名称、联系人、联系电话、合作类型（含 `typeLabel` 中文）、合作意向说明（截断）、提交时间（`metadata.creationTimestamp`）、IP、来源页面（`sourceUrl`）
- 支持分页：默认 `size=20`，可切换 10/20/50

### 2. 筛选与搜索

- 按合作类型筛选：`institution / community / home_government / channel_oem`
- 关键词搜索：匹配 `company / contact / phone` 模糊搜索
- 时间范围筛选：按提交时间区间筛选（可选，若 Halo Extension 索引不支持则前端过滤）

### 3. 详情、状态与删除

- 点击行进入详情抽屉/弹窗，展示全部字段：`company, contact, phone, type, typeLabel, message, sourceUrl, userAgent, timestamp, ip, creationTimestamp, handled`
- 状态标记：列表显示“未处理/已处理”标签，详情内可切换状态，调用 `PUT /apis/api.cooperation.vivid.run/v1alpha1/cooperations/{name}/handled` 更新 `spec.handled`，支持按“处理状态”筛选（全部/未处理/已处理）
- Extension 新增字段 `spec.handled: boolean` 默认 `false`，历史数据兼容视为 `false`
- 删除：单条删除需二次确认，调用 `DELETE /apis/api.cooperation.vivid.run/v1alpha1/cooperations/{name}`（需新增接口），删除后列表自动刷新，权限 `ROLE_ADMIN`

### 4. 导出

- 导出当前筛选结果为 CSV（UTF-8 BOM），字段同列表+详情，文件名 `cooperations-YYYYMMDD-HHmmss.csv`
- 若筛选无数据，导出按钮置灰

### 5. 空状态与反馈

- 空列表展示友好空状态，引导用户检查表单是否已发布
- 加载中、删除成功/失败、导出成功均有 Toast 提示
- 401 未登录时跳转登录，403 非管理员提示无权限

## Out of Scope

- FastAPI `server/cooperation` 整套后端（本次明确废弃，文档标注 Deprecated，主题 `endpoint` 固定指向插件路径，不再维护 `API_KEY`/SQLite 方案；`server/cooperation/` 目录保留但不再作为推荐部署路径）
- 分配跟进人、评论、工单流转等 CRM 能力（仅保留简单的已处理/未处理二态）
- Excel 多 Sheet、PDF 导出
- 批量删除/批量导出（首版不做，可后续迭代）
- 分布式限流 Redis 改造

## Acceptance Criteria

- [ ] 安装 `cooperation-plugin-*.jar` 并启用后，Halo Console 左侧可见“合作咨询”入口，无需额外配置
- [ ] 提交一条测试数据后，列表页 5 秒内可见该记录，字段完整准确
- [ ] 分页生效：`page` 切换与 `size` 切换均请求对应接口，`ListResult` 正确渲染总数
- [ ] 合作类型筛选与关键词搜索联动生效，筛选后导出 CSV 仅包含筛选结果
- [ ] 详情抽屉可查看 `message / sourceUrl / userAgent / ip / timestamp / handled` 全量信息，状态切换实时生效
- [ ] 列表支持按“处理状态”筛选，已处理/未处理标签正确显示，切换筛选后分页与导出结果一致
- [ ] 删除：点击删除→二次确认→成功后该条从列表消失，刷新后仍不存在，删除 Extension
- [ ] 导出：点击导出→下载 CSV，Excel 打开中文不乱码，列头与字段一一对应
- [ ] 未登录访问列表接口返回 401，已登录非管理员返回 403，Console 端有对应提示
- [ ] 构建产物 `cooperation-plugin-1.0.0.jar` 体积增长可控，插件启动无报错，Halo 日志无异常

## Open Questions

- 无

## Notes

- 需新增 `DELETE` 权限与接口，或复用 `ReactiveExtensionClient.delete`
- Halo Console 前端需在 `src/main/resources/console` 下提供打包产物，`plugin.yaml` 通过 `extensions` 或 `customACP` 注册菜单
- 主题端 `src/page_cooperation.html` 的 `endpoint` 保持 `/apis/api.cooperation.vivid.run/v1alpha1/cooperations`，前端校验已支持该相对路径
