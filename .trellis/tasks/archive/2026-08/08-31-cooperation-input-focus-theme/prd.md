# 修复合作表单输入框焦点样式与主题色一致

## 目标

将合作咨询页原生表单所有输入框/文本域/单选的焦点态蓝色边框与光晕修正为万椿主题色，保持与 `style.primary_color #4fc7b7` 及 `main.css @theme` 设计令牌一致。

## 背景

- 现状：`src/page_cooperation.html` 输入框为 `focus:border-primary focus:ring-primary/20 focus:ring-2`，但 `ring-primary/20` 在白底上过淡，实际感知仍为浏览器默认蓝色，单选框同理
- 期望：焦点时边框与光晕均为青绿系，与项目主色、按钮、胶囊标签一致

## 需求

### R1 输入框/文本域

- `company` `contact` `phone` 三个 `input` 与 `message` 的 `textarea`：
  - `focus:border-primary-dark`（`#2b8d89`，比 `primary` 更深更可见）或 `focus:border-primary`
  - `focus:ring-primary/30`（30% 不透明度，兼顾可见与柔和）
  - 保留 `outline-none focus:ring-2` 与 `transition`
  - 错误态 `!border-red-400` 优先级不变

### R2 单选框

- 4 项 `input[type=radio]`：`text-primary focus:ring-primary/30`，移除蓝色默认

### R3 构建与一致性

- 修改后 `pnpm check` 与 `pnpm build` 通过，产物 `templates/page_cooperation.html` 同步，无 `blue-500` 等硬编码蓝色残留

## 验收标准

- [ ] 1. 聚焦任意输入框/文本域，边框与外环均为青绿系（`#4fc7b7` 系），无蓝色
- [ ] 2. 聚焦单选框，外环为青绿
- [ ] 3. 错误态仍为红色，焦点态不覆盖错误态
- [ ] 4. `grep -rn "blue-" src/page_cooperation.html` 无命中，`grep -rn "focus:" src/page_cooperation.html` 均为 `primary` 系
- [ ] 5. `pnpm check` 与 `pnpm build` 通过

## 非目标

- 不改动校验、提交、布局与文案
