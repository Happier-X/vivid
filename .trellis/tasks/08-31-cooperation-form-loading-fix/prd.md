# 修复合作表单提交按钮常显提交中缺陷

## 目标

修复合作咨询页原生表单提交按钮在未点击时即显示“提交中...”的缺陷，确保初始态仅显示“提交合作意向”，仅在提交请求进行中显示“提交中...”并禁用按钮。

## 背景

- 现象：用户反馈未点击提交即看到“提交中...”
- 根因：`src/page_cooperation.html` 中 `btn-loading` 写为 `class="hidden inline-flex ..."`，Tailwind 生成的 `.hidden` 在 `.inline-flex` 之前，同元素两者共存时 `inline-flex` 覆盖 `hidden`，导致加载态始终可见；`src/js/cooperation-form.ts` 的 `setLoading` 仅切换 `hidden` 未处理 `inline-flex`
- 影响：首屏即错误态，用户无法区分是否已提交

## 需求

### R1 模板层

- `src/page_cooperation.html` 中 `#cooperation-submit` 的 `btn-loading` 初始类移除 `inline-flex`，改为 `class="hidden items-center gap-2"`（或仅 `hidden`），确保初始隐藏

### R2 逻辑层

- `src/js/cooperation-form.ts` 的 `setLoading(loading)` 同时切换 `hidden` 与 `inline-flex`：
  - `loading=true`：`btn-text` 加 `hidden`，`btn-loading` 移除 `hidden` 并添加 `inline-flex`，`disabled=true` + `aria-busy=true`
  - `loading=false`：反向切换，恢复 `btn-text` 可见，`btn-loading` 隐藏并移除 `inline-flex`，`disabled=false`

### R3 兼容

- 不改动校验、提交、成功态等其他逻辑
- 保持 `pnpm check` 与 `pnpm build` 通过，产物 `templates/page_cooperation.html` 同步修复

## 验收标准

- [ ] 1. 初始加载页面，按钮仅显示“提交合作意向”，不显示“提交中...”与转圈
- [ ] 2. 点击提交（校验通过且 endpoint 已配置）后，按钮文案切换为“提交中...”并禁用，请求结束恢复
- [ ] 3. 未配置 endpoint、校验失败分支不触发 loading
- [ ] 4. `grep -n "btn-loading" src/page_cooperation.html` 初始类不含 `inline-flex`
- [ ] 5. `pnpm check` 与 `pnpm build` 通过

## 非目标

- 不调整样式设计与后端接口
