# 产品详情改为页面模板复用

## 目标

为精选产品 3 款（体征监测仪 / 跌倒监测仪 / 紧急按钮）提供硬编码的单页模板 `page_product.html`，每款产品在 Halo 后台新建一个自定义页面并选用该模板即呈现对应详情，无需维护富文本，所有文案与图库直接在代码中硬编码，后续新增产品直接改代码复用。

## 背景与已确认事实

- 技术栈：Halo 主题 Vite+Thymeleaf，`theme.yaml` 已注册 3 个自定义页面模板
- 现状：`post.html` 的文章式产品详情已实现但被用户否决，用户明确要求改为页面模板且“更简，不需要自己维护富文本了，直接就是改这个代码”
- 约束：产品少（3 款），版式固定，接受硬编码

## 需求

### R1 模板注册与复用

- `theme.yaml` 新增 `customTemplates.page` 条目：`name: 产品详情`、`file: page_product.html`
- `src/page_product.html` 单一模板硬编码 3 款产品的详情，通过 `singlePage.spec.slug`（或 `title` 关键词）做 `th:if`/`th:switch` 分支，匹配则渲染对应产品块，未匹配回退通用占位

### R2 版式（硬编码极简）

- 固定区块（按 1:1 复刻产品详情高级感）：
  1. 面包屑（首页 / 产品详情 / 当前标题）+ 返回
  2. 首图大图（`src/assets/images/wanchun/product-*.png/jpg`）
  3. 标题 + 一句话卖点
  4. 产品介绍（3-4 条卡片化亮点，直接写死）
  5. 规格参数表（硬编码表格，`border-line` 统一样式）
  6. 相关产品 3 卡（硬编码链接到另外 2 款 + 1 占位）
  7. 合作 CTA（→ `/cooperation`）
- 所有文案与图片路径直接写在 `page_product.html` 内，不读取 `singlePage.content.content`

### R3 链路

- `src/partials/section-products.html` 3 卡链接由动态 `postFinder` 改为硬编码 `th:href="@{/product-vital}"` 等 3 个固定 SinglePage slug（`product-vital` / `product-diedao` / `product-sos`），与用户后台创建的页面 slug 一致
- 无该页面时 404 由 Halo 处理

## 非目标

- 富文本维护（`singlePage.content.content` 本任务忽略）
- 文章分类与 `postFinder` 链路
- 商城交易链路

## 验收标准

- [ ] 1. `theme.yaml` 含 `产品详情 -> page_product.html`，`src/page_product.html` 存在且硬编码 3 款产品分支
- [ ] 2. 后台新建 3 个页面，Slug 分别为 `product-vital` / `product-diedao` / `product-sos`，选用模板“产品详情”后，访问 `/product-vital` 等即见对应硬编码详情
- [ ] 3. 首页精选 3 卡点击分别跳 `/product-vital` `/product-diedao` `/product-sos`
- [ ] 4. 详情页含首图、产品介绍卡片、规格表、相关产品与 CTA，版式与现有产品详情一致
- [ ] 5. `pnpm check` 与 `pnpm build` 通过，产物 `templates/page_product.html` 存在

## 约束

- 遵循 `halo-theme.md` 双层模板与 `layout.html` 注入
- 硬编码文案后续新增产品需改代码
