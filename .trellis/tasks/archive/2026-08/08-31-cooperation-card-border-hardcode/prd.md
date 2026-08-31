# 修复合作类型选中卡片外边框蓝色为青绿

## 目标

将合作类型 4 个选中卡片的外边框由 `var(--color-primary)` 变量改为硬编码 `#4fc7b7`，彻底消除蓝色。

## 背景

- 现状：`label` 为 `has-[:checked]:border-primary`，依赖控制台 `primary_color`，若为蓝色或 `:has` 优先级不足则仍蓝；圆点已硬编码但外框未
- 期望：选中卡片边框与背景均为青绿系

## 需求

### R1 模板硬编码

- 4 个 `label` 由 `has-[:checked]:border-primary` 改为 `has-[:checked]:border-[#4fc7b7]` 并加 `!` 必要时 `has-[:checked]:!border-[#4fc7b7]`
- 保留 `has-[:checked]:bg-mist`（浅青 `#eef9f7`）

### R2 CSS 兜底

- `src/css/main.css` 追加 `label:has(input:checked)` 强覆盖 `border-color:#4fc7b7 !important`

## 验收标准

- [ ] 1. 选中任意类型，卡片外框为 `#4fc7b7` 青绿，无蓝
- [ ] 2. `grep -n "has.*border" src/page_cooperation.html` 含 `#4fc7b7`
- [ ] 3. `pnpm check/build` 通过
