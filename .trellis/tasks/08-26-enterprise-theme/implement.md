# 实施计划：企业官网风格 Halo 主题

## 前置

- [ ] 阅读技能参考文档：references/templates.md、global-variables.md、finder-apis.md（重点核对 postFinder.list 参数名与返回结构）

## 阶段 1：工具链接入 TailwindCSS 4

- [ ] `pnpm add -D tailwindcss @tailwindcss/vite`
- [ ] vite.config.ts plugins 增加 `tailwindcss()`（置于 haloThemePlugin 之前）
- [ ] src/css/main.css 改为 `@import "tailwindcss"` 并定义 @theme 设计令牌（primary 色、圆角、字体栈）
- [ ] 验证：`pnpm build` 成功，产物 CSS 含 tailwind 工具类

## 阶段 2：配置层 settings.yaml + theme.yaml

- [ ] 按 design.md 分组设计重写 settings.yaml（style/hero/features/products/intro/cta/stats/footer）
- [ ] 使用 Halo 扩展输入类型：switch、attachment、array、list、categorySelect、number
- [ ] theme.yaml 元信息更新（displayName、description 等）
- [ ] 验证：YAML 语法可解析；字段命名分组清晰

## 阶段 3：全局布局

- [ ] partials/layout.html 重构：引入 header/footer partial、内联 CSS 变量注入（theme.config.style）
- [ ] partials/header.html：logo + 主菜单遍历（menuFinder.getPrimary()）+ 移动端汉堡按钮
- [ ] partials/footer.html：版权/备案/链接 + halo:footer
- [ ] src/js/main.ts：移动端菜单开合逻辑
- [ ] 验证：临时在 index.html 输出占位内容确认布局渲染

## 阶段 4：首页区块

- [ ] partials/section-hero.html（含特性标签 list 渲染、双 CTA、主图 attachment）
- [ ] partials/section-features.html（编号卡片 + 配图）
- [ ] partials/section-products.html（categorySelect + postFinder 查询 + post-card 网格）
- [ ] partials/post-card.html 重写（封面图 + 分类徽标 + 标题 + 摘要）
- [ ] partials/section-intro.html（左文右图 + 功能点数组）
- [ ] partials/section-cta.html（渐变横幅双按钮）
- [ ] partials/section-stats.html（统计项数组）
- [ ] index.html 按序组装，各区块包 th:if 开关
- [ ] 验证：pnpm build 成功；逐区块检查 Thymeleaf 表达式安全导航（?.）使用

## 阶段 5：内页模板

- [ ] post.html：详情版式（标题/元信息/正文排版/上下篇）
- [ ] page.html：自定义页面版式
- [ ] category.html / categories.html / tag.html / tags.html / archives.html 统一新视觉
- [ ] 验证：pnpm build 成功

## 阶段 6：整体验收

- [ ] `pnpm check` 通过
- [ ] `pnpm build` 通过，产出 theme 包
- [ ] 对照 prd.md 验收标准 1–8 逐项自查（有 Halo 实例则实际安装验证）

## 验证命令

```bash
pnpm check   # vp check --fix
pnpm build   # tsc && vp build && theme-package
```

## 风险文件与回滚点

- 每阶段完成后单独 commit（回滚点）
- 高风险文件：settings.yaml（表单结构）、src/partials/layout.html（全局影响）、vite.config.ts（构建链）

## 提交前检查

- [ ] templates/ 已在 .gitignore
- [ ] 无硬编码演示文案残留（应来自默认配置值或示例内容）
