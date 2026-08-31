# 合作表单输入框焦点蓝色强制覆盖为主题色

## 目标

通过 `!important` 与 `:-webkit-autofill` 强覆盖，彻底消除合作表单输入框在聚焦与自动填充时出现的蓝色边框/光晕/底色，确保与 `theme.config.style.primary_color #4fc7b7` 青绿主题一致，并通过 bump 版本强制刷新线上缓存。

## 背景

- 已将 `src/page_cooperation.html` 由 `focus:ring/20` 改为 `30` 且 `border-primary-dark`，本地编译 `main-DuMwvJwu.css` 已为青绿（`#2b8d89` / `#4fc7b74d`），但线上仍为蓝色，疑为浏览器默认 `outline/autofill` 蓝底未被覆盖或旧包缓存
- 需在 `src/css/main.css` 增加高优先级覆盖，并将 `theme.yaml version 1.0.0 -> 1.0.1` 以 bust 缓存

## 需求

### R1 强覆盖焦点态

- 在 `src/css/main.css` 末尾新增：
  ```css
  #cooperation-form input:focus,
  #cooperation-form textarea:focus {
    border-color: var(--color-primary) !important;
    box-shadow: 0 0 0 2px color-mix(in oklab, var(--color-primary) 30%, transparent) !important;
    outline: none !important;
  }
  #cooperation-form input[type="radio"]:focus {
    --tw-ring-color: var(--color-primary) !important;
  }
  ```

### R2 覆盖自动填充蓝底

- 新增：
  ```css
  #cooperation-form input:-webkit-autofill,
  #cooperation-form textarea:-webkit-autofill {
    box-shadow: 0 0 0 1000px #fff inset !important;
    -webkit-text-fill-color: var(--color-ink-900) !important;
    transition: background-color 5000s ease-in-out 0s;
  }
  ```

### R3 版本与打包

- `theme.yaml version: 1.0.1`
- 执行 `pnpm build`，产物 `dist/theme-vite-starter-1.0.1.zip` 与 `templates/assets/main-*.css` 更新，`templates/page_cooperation.html` 仍为青绿

## 验收标准

- [ ] 1. `src/css/main.css` 含上述两段 `!important` 覆盖
- [ ] 2. `theme.yaml version` 为 `1.0.1`
- [ ] 3. `templates/assets/main-*.css` 含 `cooperation-form input:focus` 规则
- [ ] 4. `templates/page_cooperation.html` 输入框类仍为 `primary-dark/30`
- [ ] 5. `pnpm check` 与 `pnpm build` 通过，`dist` 含新版本 zip

## 非目标

- 不改动校验与提交逻辑
