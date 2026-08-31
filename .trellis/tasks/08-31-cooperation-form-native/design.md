# 技术设计：合作咨询表单去飞书化-自建原生表单对接

## 架构总览

```
Halo Console (settings.yaml → ConfigMap)
        ↓  theme.config.cooperation.*
Thymeleaf 渲染 src/page_cooperation.html
        ↓  data-endpoint / data-receiver-email 注入 DOM
Browser 原生表单 + src/js/cooperation-form.ts
        ↓  fetch POST JSON
外部 Endpoint (Cloudflare Worker 示例)
        ↓  校验 → 限流 → 发邮件(Resend/SMTP) → 返回 JSON
```

主题保持纯静态，所有服务端逻辑外置到可配置 `endpoint`，主题仅负责 UI/校验/提交编排。

## 配置层设计

### settings.yaml 新增分组

```yaml
- group: cooperation
  label: 合作咨询表单
  formSchema:
    - $formkit: text
      name: endpoint
      label: 表单提交接口
      value: ""
      help: "例如 https://xxx.workers.dev/api/cooperation，留空则前端仅展示电话/邮箱联系提示"
    - $formkit: text
      name: receiver_email
      label: 接收邮箱（备用 mailto）
      value: "contact@wanchunsmart.com"
    - $formkit: text
      name: success_title
      label: 提交成功标题
      value: "提交成功"
    - $formkit: textarea
      name: success_desc
      label: 提交成功描述
      value: "我们已收到您的合作意向，商务团队将在 24 小时内与您联系。"
```

### 模板契约

- 读取：`theme.config.cooperation.endpoint`、`receiver_email`、`success_title`、`success_desc`
- 注入：在表单容器上 `th:attr="data-endpoint=${theme.config.cooperation.endpoint}, data-receiver-email=${theme.config.cooperation.receiver_email}, data-success-title=${theme.config.cooperation.success_title}, data-success-desc=${theme.config.cooperation.success_desc}"`
- JS 中仅通过 `dataset` 读取，不硬编码
- 默认值兜底：`endpoint ?: ""`，`receiver_email ?: "contact@wanchunsmart.com"`

## 模板层

### src/page_cooperation.html 改动

- 删除区块：`div.border-line.overflow-hidden > iframe` 整体移除
- 启用并重构注释内的 `form`：
  - 保留原有 `grid sm:grid-cols-2` 的 company/contact，phone 单独一行，type radio 2列网格，message textarea
  - 外层容器 `id="cooperation-form-root"` 承载 `data-*`
  - 表单 `id="cooperation-form"`，新增 `novalidate` 由 JS 接管校验
  - 每个输入下方预留 `<p data-error-for="company" class="hidden text-xs text-red-500 mt-1"></p>`
  - 蜜罐：`<input name="website" tabindex="-1" autocomplete="off" class="hidden" aria-hidden="true">`
  - 按钮：`id="cooperation-submit"`，内含 `span[data-role="btn-text"]` 与 `span[data-role="btn-loading" class="hidden"]`
  - 成功态：`div#cooperation-success.hidden` 含图标+标题+描述+再次提交按钮 `#cooperation-reset`
  - 错误条：`div#cooperation-error.hidden` 位于表单顶部
  - 底部提示：保留“提交即表示您同意...”文案
  - 未配置提示：`div#cooperation-no-endpoint` 当 `endpoint` 为空时由 JS 显示

- 右侧三横卡与 Why 卡不动

### src/js/cooperation-form.ts 新增

职责单一：表单校验 + 提交编排，不引入框架。

- 导出 `initCooperationForm()`，在 `src/js/main.ts` 中 `DOMContentLoaded` 时调用
- 校验规则：
  - `company: required, 2-50`
  - `contact: required, 2-20`
  - `phone: required, /^(1[3-9]\d{9}|0\d{2,3}-?\d{7,8})$/`
  - `type: required`
  - `message: 0-500`
  - `website: must empty`
- 工具函数：`validateField(name, value) -> string | null`，`showFieldError`，`clearFieldError`，`validateAll() -> boolean`
- 提交流程：
  ```ts
  onSubmit(e):
    e.preventDefault()
    if (!validateAll()) return
    if (website.value) return // 蜜罐静默成功
    if (!endpoint) { showNoEndpoint(); return }
    setLoading(true)
    try {
      const res = await fetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ company, contact, phone, type, typeLabel: labelMap[type], message, website, sourceUrl: location.href, userAgent: navigator.userAgent, timestamp: new Date().toISOString() }),
        signal: AbortSignal.timeout(10000)
      })
      const data = await res.json().catch(() => ({}))
      if (res.ok && data.success !== false) showSuccess()
      else showError(data.message || "提交失败，请稍后重试或通过电话/邮箱联系")
    } catch {
      showError("网络异常，请检查网络后重试或通过电话/邮箱联系")
    } finally {
      setLoading(false)
    }
  ```
- 成功态：隐藏 `form`，显示 `#cooperation-success`，填充 `success_title/desc` from dataset
- 再次提交：重置表单、清空错误、切回表单态

### src/js/main.ts 改动

- `import { initCooperationForm } from "./cooperation-form"`
- `initCooperationForm()` 幂等调用（若页面无对应 DOM 则直接 return）

### 样式

- 无新增 CSS 文件，复用现有 Tailwind 4 工具类与 `main.css` 设计令牌
- 错误态：`!border-red-400`，成功态卡片复用 `rounded-[12px] border bg-white`

## 后端参考实现

### worker/cooperation.ts

- 运行时：Cloudflare Workers (兼容 Workerd)，`export default { async fetch(request, env, ctx) }`
- `env` 变量：
  - `RECEIVER_EMAIL`（必填）
  - `RESEND_API_KEY`（与 `SMTP_*` 二选一）
  - `SMTP_HOST/PORT/USER/PASS`（二选一）
  - `ALLOWED_ORIGINS`（逗号分隔，空则允许所有）
  - `RATE_LIMIT_SECONDS` 默认 60
- 逻辑：
  1. `OPTIONS` 预检：返回 `Access-Control-Allow-Origin/Methods/Headers`，Origin 校验
  2. 仅允许 `POST application/json`
  3. 解析 body，服务端复用同一校验规则 + 蜜罐
  4. 限流：`caches.default` 或内存 `Map`（Worker 单实例内存，简化实现，用 `globalThis` Map + timestamp）
  5. 组装邮件：标题 `【万椿官网】合作咨询 - {company} - {typeLabel}`，正文含所有字段 + sourceUrl + timestamp
  6. 发信：`Resend` 优先（`fetch https://api.resend.com/emails`），回退 `SMTP`（需 `nodemailer` 兼容层，文档说明如需 SMTP 请改用自建 Node 服务，Worker 示例默认 Resend）
  7. 返回 `200 { success: true }` 或 `400/429/500 { success: false, message }`
  8. CORS 头回写 `Access-Control-Allow-Origin: <origin>`（若配置了白名单则严格匹配）

### worker/README.md

- 说明：部署步骤（`wrangler deploy`）、环境变量配置、本地 `wrangler dev` 调试、前端 `settings.yaml` 填入对应 URL
- 说明限流与邮件二选一，预留飞书 webhook 扩展代码注释

## 数据契约

### 前端 → 后端

```ts
type CooperationPayload = {
  company: string;
  contact: string;
  phone: string;
  type: "institution" | "community" | "home_government" | "channel_oem";
  typeLabel: string; // 冗余便于邮件直读
  message: string;
  website: string; // 蜜罐
  sourceUrl: string;
  userAgent: string;
  timestamp: string; // ISO8601
};
```

### 后端 → 前端

```ts
type CooperationResponse = { success: boolean; message?: string };
```

## 兼容与降级

- 无 JS：表单可见但提交按钮点击提示“请启用 JavaScript”；`noscript` 块展示电话/邮箱
- `endpoint` 为空：JS 拦截提交，显示 `cooperation-no-endpoint` 提示，按钮文案改为“请电话/邮件联系”

## 构建与验证

- `pnpm check`（`vp check --fix`）必须通过
- `pnpm build`（`tsc && vp build && theme-package`）产物 `templates/page_cooperation.html` 无 `iframe/feishu`
- 手动验证：在 Halo 本地实例修改 `cooperation.endpoint`，前台 `data-endpoint` 即时变化

## 风险与回滚

- 风险：CORS 配置错误导致前端跨域失败 → 提供 `ALLOWED_ORIGINS` 文档与 `*` 兜底说明
- 回滚：单 commit 包含全部改动，`git revert` 即可恢复 `iframe` 版本；或在 `settings.yaml` 清空 `endpoint` 临时降级为展示态
