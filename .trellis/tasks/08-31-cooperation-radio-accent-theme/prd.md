# 修复合作类型单选选中蓝色为主题色

## 目标

将合作咨询页合作类型 4 个单选框选中后的蓝色圆点、蓝色边框修正为万椿主题色，保持与 `primary #4fc7b7` 一致。

## 背景

- 现状：`src/page_cooperation.html` 单选为 `text-primary focus:ring-primary/30 border-gray-300`，部分浏览器下 `text-primary` 不改 `accent-color`，圆点仍为系统蓝；外层 `has-[:checked]:border-primary` 在 `:has` 未生效时闪蓝
- 期望：选中态圆点、边框、光晕均为青绿

## 需求

### R1 单选输入

- 4 个 `input[type=radio]` 由 `text-primary` 改为 `accent-primary`（或 `accent-[#4fc7b7]` + `checked:accent-primary`），并保留 `focus:ring-primary/30`
- 追加 `accent-primary` 确保 `accent-color: var(--color-primary)` 生效

### R2 外层卡片

- `label` 的 `has-[:checked]:border-primary has-[:checked]:bg-mist` 提升为 `has-[:checked]:!border-primary`（或 `has-[:checked]:border-primary` 已足够，验证后决定），确保选中边框为青绿

### R3 兜底

- `src/css/main.css` 追加 `#cooperation-form input[type="radio"]:checked { accent-color: #4fc7b7 !important; }` 作为兜底

## 验收标准

- [ ] 1. 选中任意合作类型，圆点为青绿 `#4fc7b7` 系，无蓝
- [ ] 2. 选中卡片边框为青绿，`bg-mist` 浅青底
- [ ] 3. `grep -n "accent-primary" src/page_cooperation.html` 命中 4 处
- [ ] 4. `pnpm check/build` 通过
