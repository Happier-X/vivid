# 合作表单焦点硬编码青绿并 bump 至 1.0.2

## 目标

将 `src/css/main.css` 中合作表单焦点覆盖由 `var(--color-primary)` 改为硬编码 `#4fc7b7`/`#2b8d89`，确保即使 CSS 变量未注入也为青绿，并将 `theme.yaml version 1.0.1 -> 1.0.2` 强制刷新线上缓存。

## 背景

- `1.0.1` 已用 `var(--color-primary)` 硬覆盖，但线上仍蓝，疑为变量未在输入框作用域生效或旧包缓存
- 需去变量化，直接写死品牌色，并再次 bump 版本 bust 缓存

## 需求

### R1 样式硬编码

- `src/css/main.css` 中 `#cooperation-form input:focus, textarea:focus` 的 `border-color` 由 `var(--color-primary)` 改为 `#4fc7b7`
- `box-shadow` 中 `color-mix(... var(--color-primary) ...)` 改为 `color-mix(in oklab, #4fc7b7 30%, transparent)` 或直接 `rgba(79,199,183,0.3)`
- `input[type=radio]:focus` 的 `--tw-ring-color` 由 `var(--color-primary)` 改为 `#4fc7b7`

### R2 版本

- `theme.yaml version: 1.0.2`

### R3 构建

- `pnpm build` 生成 `main-*.css` 含硬编码青绿，`dist/theme-vite-starter-1.0.2.zip`

## 验收标准

- [ ] 1. `src/css/main.css` 含 `#4fc7b7` 硬编码
- [ ] 2. `theme.yaml version 1.0.2`
- [ ] 3. `templates/assets/main-*.css` 含 `#4fc7b7` 且无 `blue-500`
- [ ] 4. `pnpm check/build` 通过
