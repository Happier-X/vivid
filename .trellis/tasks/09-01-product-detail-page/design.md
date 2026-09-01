# 技术设计：产品详情页开发

## 架构总览

沿用 Vite + `vite-plugin-halo-theme` 双层模板，不改构建体系。核心为 `post.html` 的产品分支增强 + `section-products.html` 的动态化。

```
settings.yaml (无新增)
  ↓ Halo 渲染
post.html (Thymeleaf 运行时)
  ├─ 判断 isProduct = 单篇所属 categories 含 "产品中心"
  ├─ isProduct ? 渲染产品区块 : 渲染通用文章
  └─ 相关产品 via postFinder.list({categoryName:"产品中心"})
section-products.html (首页)
  └─ postFinder.list({categoryName:"产品中心", size:3}) 动态 3 卡，回退静态
```

## 数据流与契约

### 详情页 `post.html`

- 输入：`post` (`PostVo`), `post.spec.title/cover`, `post.content.content` (HTML), `post.categories`, `postFinder`, `categoryFinder`
- 判断：
  ```html
  th:with="isProduct=${not #lists.isEmpty(post.categories) and
  #lists.contains(post.categories.![spec.displayName], '产品中心')}"
  ```
  （实现时核对 `categories` 结构，需安全导航 `?.`）
- 卖点/规格/图库抽取：对 `post.content.content` 的 HTML 字符串做 `contains('## 规格参数')` 类判断，`th:utext` 原样渲染时通过 CSS 美化表格；或用 `th:if` 包裹整块，空时隐藏
- 相关产品：
  ```html
  th:with="relatedPosts=${postFinder.list({page:1, size:4, categoryName:'产品中心'})}"
  ```
  需核对 `postFinder.list` 实际参数名（`page/size/categoryName` vs `category`），实现前查 `finder-apis.md` 并以在线文档为准，过滤 `post.metadata.name != related.metadata.name` 取前 3

### 首页 `section-products.html`

- `th:with="productPosts=${postFinder.list({page:1,size:3,categoryName:'产品中心'})}"`
- `th:each` 渲染 3 卡：`spec.cover/title`, `status.excerpt/permalink`, `spec.displayName`
- 无数据时 `th:if="${#lists.isEmpty(productPosts.items)}"` 回退静态占位（保留当前 3 卡静态作为 fallback，避免空网格）

## 模板结构

```
src/
├── post.html                      // 扩展：通用 + 产品分支
├── partials/
│   ├── section-products.html      // 动态化 3 卡
│   └── product/
│       ├── product-gallery.html   // 可选：图库轮播片段（include）
│       └── product-specs.html     // 可选：规格表片段
└── css/main.css                   // 新增 .product-specs table 样式（如需）
```

是否拆 `product/` 片段视复杂度定，首期可直接在 `post.html` 内联，避免过度拆分。

## 样式与交互

- 表格：`.product-specs` 用 `border-line` 1px 表格，首列 `bg-mist`，`th` 深色
- 图库：`aspect-[4/3]` 主图 + 下方缩略图横滑，`src/js/product-gallery.ts`（可选，首期可用纯 CSS 横滑，无 JS）
- CTA：`btn btn-primary` 跳 `/cooperation`
- 相关产品：复用 `post-card.html` 或精简版 3 卡网格 `lg:grid-cols-3`

## 兼容与降级

- 非“产品中心”文章：`isProduct=false`，不渲染产品区块，保持现有文章版式不变
- 无“产品中心”分类或空列表：首页 3 卡回退静态，详情相关推荐整块 `th:if` 隐藏
- `postFinder.list` 参数名以在线文档为准，不凭记忆硬编码；所有 `th:*` 加 `?.` 安全导航

## 回滚

- 单 commit 包含 `post.html` + `section-products.html` + `main.css`（如有），`git revert` 即可回退为通用文章版式
- 静态回退保证无分类时页面不崩

## 风险与权衡

- 约定式内容解析：优点零插件，缺点依赖作者按标题书写；权衡后首期接受该约定，二期可迁移为插件字段
- `has-[:checked]` 等现代 CSS 在旧浏览器降级为普通态，不影响功能
