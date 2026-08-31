# 合作咨询表单去飞书化-自建原生表单对接

## 目标

将 `page_cooperation.html` 中基于 `iframe` 的飞书多维表格表单替换为主题自建原生表单，提供与万椿视觉一致的交互体验，并通过 `settings.yaml` 可配置的后端接口实现数据提交、邮件通知与防刷能力，彻底移除对飞书 `iframe` 的依赖。

## 背景与现状

- 现状：`src/page_cooperation.html` 左侧表单区使用 `iframe src="https://my.feishu.cn/share/base/form/..." height 700px` 嵌入飞书表单，存在加载慢、样式不可控、移动端体验差、与品牌视觉割裂问题
- 已有资产：模板内注释掉一套原生 `mailto` 表单（字段：公司名称/联系人/联系电话/合作类型/合作意向说明），样式与当前卡片体系一致，可直接复用并增强
- 约束：Halo 主题为纯静态模板，无服务端运行能力，提交必须依赖外部可配置 `endpoint`（Serverless / 自建 API / 第三方表单服务）

## 需求

### R1 原生表单 UI 与校验

- 字段（与原有 `mailto` 保持一致，不新增邮箱字段避免用户抵触）：
  - 公司名称 `company`：必填，2-50字符
  - 联系人 `contact`：必填，2-20字符
  - 联系电话 `phone`：必填，支持中国大陆手机号 `1[3-9]\d{9}` 与座机 `0\d{2,3}-?\d{7,8}`，前端实时校验
  - 合作类型 `type`：必填，单选 4 项 `institution | community | home_government | channel_oem`，默认选中 `institution`
  - 合作意向说明 `message`：选填，0-500字符，`textarea 4行`
  - 蜜罐字段 `website`（隐藏）：选填，若有值则判定为机器人静默丢弃
- 交互：
  - 行内校验提示（失焦与提交时触发），错误态 `border-red-400 + 文字提示`
  - 提交中按钮 `disabled + loading 文案/转圈`，防重复提交
  - 成功态：表单区替换为成功卡片（图标+标题+描述+“再次提交”按钮），3秒内不自动重置为输入态
  - 失败态：顶部错误条 + 按钮恢复可点，保留已填内容
  - 底部保留“提交即表示您同意我们与您联系”提示
- 样式：复用现有卡片容器 `rounded-[12px] border-[rgba(217,236,233,0.9)] bg-white shadow` 与输入框 `rounded-[8px] border-line focus:border-primary` 体系，响应式保持 `sm:grid-cols-2`

### R2 提交与后端对接

- 提交载荷 `POST application/json` 到 `theme.config.cooperation.endpoint`：
  ```json
  {
    "company": "",
    "contact": "",
    "phone": "",
    "type": "",
    "typeLabel": "",
    "message": "",
    "website": "",
    "sourceUrl": "",
    "userAgent": "",
    "timestamp": ""
  }
  ```
- 前端行为：
  - 当 `endpoint` 已配置（非空且为 http/https）：`fetch` 提交，处理 `2xx/非2xx/网络异常` 三分支
  - 当 `endpoint` 未配置：按钮置灰并提示“表单提交功能未配置，请通过右侧电话/邮箱联系”，同时提供 `mailto` 备用链接 `contact@wanchunsmart.com`
  - 请求超时 10s，失败保留表单数据
  - `CORS` 由后端负责，前端不做 `no-cors`
- 后端参考实现：提供一个可直接部署的 `Cloudflare Worker` 示例（`worker/cooperation.ts`），能力：
  - 校验必填与 phone 正则、蜜罐、message 长度
  - 内存级限流（同一 IP 1分钟1次）
  - 转发邮件（`Resend` 或 `SMTP` 二选一，环境变量配置）
  - 可选写入日志/飞书 webhook（预留扩展点，默认注释）
  - 返回统一 JSON `{ success: boolean, message: string }`

### R3 主题配置扩展

- `settings.yaml` 新增分组 `cooperation`：
  - `endpoint`：`text` 类型，默认值空，`help: 合作表单提交接口地址，例如 https://xxx.workers.dev/api/cooperation，留空则表单仅展示联系方式提示`
  - `receiver_email`：`text` 类型，默认值 `contact@wanchunsmart.com`，用于文档提示与 `mailto` 备用
  - `success_title / success_desc`：`text / textarea` 可选，提供成功态文案可配置（若不做则直接模板硬编码，需在设计中定夺；本 PRD 要求可配置）
- 模板中通过 `th:attr="data-endpoint=${theme.config.cooperation.endpoint}"` 等方式将配置注入前端 JS，不在 JS 中硬编码

### R4 移除飞书依赖

- 删除 `iframe` 标签及外层 `border-line overflow-hidden` 容器中仅为 `iframe` 服务的结构
- 全库 `grep` 无 `feishu.cn`、`my.feishu.cn`、`iframe` 残留于合作页
- 右侧“更多联系方式”与“为什么选择万椿”区块保持不变

### R5 可访问性与兼容性

- 所有输入有 `label for` 关联，radio 组有 `fieldset/legend`
- 键盘可完整操作，`Enter` 提交，`Esc` 关闭成功态（如有弹窗则无需）
- 无 JS 时表单仍可见，提交按钮提示“请启用 JavaScript 后提交或通过电话/邮件联系”

## 非目标（Out of Scope）

- Halo Java 插件后端（不在本主题仓库实现）
- 后台查看提交记录的 Admin UI
- 短信验证码、附件上传、富文本
- 多语言

## 验收标准

- [ ] 1. `src/page_cooperation.html` 已无 `iframe` 与 `feishu.cn` 引用，`pnpm build` 后 `templates/page_cooperation.html` 同样无残留
- [ ] 2. 表单字段、校验规则、蜜罐、默认选中与 PRD R1 一致；空值/非法手机号可复现行内错误提示
- [ ] 3. 已配置 `endpoint` 时，提交触发 `fetch POST JSON`，成功显示成功态并可“再次提交”，失败显示错误条且保留数据；提交中按钮不可重复点击
- [ ] 4. 未配置 `endpoint` 时，表单展示配置缺失提示且提供 `mailto:receiver_email` 备用，`fetch` 不被调用
- [ ] 5. `settings.yaml` 新增 `cooperation` 分组，后台主题设置页可见且修改后前台 `data-endpoint` 即时生效（无需重新构建）
- [ ] 6. `pnpm check` 与 `pnpm build` 均通过，无新增 lint 报错
- [ ] 7. 提供 `worker/cooperation.ts` 参考实现与 `worker/README.md` 部署说明（含环境变量、限流、邮件服务二选一）
- [ ] 8. 移动端 375px 与桌面端 1180px 下布局无错位，输入框与卡片圆角/阴影与现有视觉一致

## 约束与依赖

- 遵循 `.trellis/spec/frontend/halo-theme.md`：`settings.yaml` 字段与 `theme.config.*` 引用必须一一对应，改动双侧同步检查
- 静态资源路径相对 `src/` 书写，`src/partials` 隔离
- 主题不存储用户数据，所有提交均转发至外部 `endpoint`

## 备注

- 轻量任务但涉及前后端契约，需补充 `design.md` 与 `implement.md` 后再 `task.py start`
