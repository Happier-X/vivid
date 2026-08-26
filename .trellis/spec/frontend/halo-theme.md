# Halo 主题开发规范

> 本项目是 Halo CMS 主题（Vite + vite-plugin-halo-theme + TailwindCSS 4 + Thymeleaf）。以下为实际开发中验证过的约定。

---

## 1. 双层模板体系（最易混淆点）

| 语法 | 处理时机 | 说明 |
|------|---------|------|
| `<include src>` / `<slot>` / `<template name>` | **Vite 构建期** | 组件复用机制，与 Thymeleaf 完全隔离 |
| `th:*` 属性 | **Halo 运行时** | 服务端渲染，构建产物 templates/ 中保留 |

- partials 内引用静态资源路径**一律相对 `src/` 书写**（如 `./css/main.css`），与文件所在位置无关
- `src/partials/` 不会被当作页面入口；其余 `src/*.html` 自动成为构建入口并输出到 `templates/`

## 2. settings.yaml ↔ 模板契约

- 访问模式：`theme.config.<group>.<field>`，**模板中每个引用必须能在 settings.yaml 中找到对应定义**（改动任一侧后需交叉比对）
- 首页区块统一约定：每个区块分组内用 `switch` 类型字段 `enabled` 控制显隐；模板中 `th:block th:if="${theme.config.<group>.enabled}"` 包裹整个 include
- 多条目配置用 FormKit `array`（对象数组）/ `list`（字符串数组）；图片用 `attachment`；分类选择用 `categorySelect`（返回 metadata.name）
- 所有字段必须提供合理默认值，默认文案保持通用企业风格、不得包含参考站品牌
- FormKit 陷阱：array/list 子节点内引用兄弟字段用 `$value.xxx` 而非 `$get()`；带 `if` 的节点必须声明唯一 `key`

## 3. 设计令牌与后台配色联动

- `src/css/main.css` 用 Tailwind 4 `@theme` 定义令牌（`--color-primary` 等），工具类引用同一变量
- `layout.html` head 内联 `<style th:inline="css">` 把 `theme.config.style.*` 写入 `:root` 变量实现运行时换肤——**改后台颜色无需重新构建**

## 4. Finder API 使用纪律

- **禁止凭记忆写参数名**：postFinder/menuFinder 等方法签名随版本演进，写代码前必须核对在线文档（见技能 references/finder-apis.md 链接）
- 已核实：`postFinder.list({ page, size, categoryName })` 返回 `ListResult<ListedPostVo>`；卡片字段：`spec.cover/title`、`status.excerpt/permalink`、`categories[0].spec.displayName`
- 可空数据源必须安全导航：如主菜单未配置时 `${menu.menuItems}` 会 NPE，须写 `menu?.menuItems`
- 区块/列表必须有空状态处理（空列表提示或整块隐藏，避免残留空色带）

## 5. 构建与验证

```bash
pnpm check   # vp check --fix（提交前必跑）
pnpm build   # tsc && vp build && theme-package（产物 templates/ + dist/*.zip）
```

- pre-commit 钩子对纯 YAML/MD 提交会报 "No files found to lint"，此时手动确认 `pnpm check` 通过后可用 `--no-verify`
- 待办：可将 vite.config.ts 的 `staged` 配置改为仅匹配可 lint 文件以消除该问题

## 6. 禁止事项

- ❌ 在模板中硬编码演示文案（应来自 settings 默认值）
- ❌ 凭训练数据书写 Halo VO 字段名或 Finder 方法签名
- ❌ 手写 meta 标签（Halo 运行时注入）；正文输出必须 `th:utext`
- ❌ 改动 settings.yaml 字段名而不同步检查模板中的 theme.config 引用
