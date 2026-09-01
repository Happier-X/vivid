# 技术设计：产品详情改为页面模板复用

## 架构总览

沿用 Vite 双层模板，单页模板硬编码，无后端数据依赖。

```
theme.yaml customTemplates.page 新增 产品详情 -> page_product.html
  ↓ Halo SinglePage 渲染
src/page_product.html
  ├─ th:switch="${singlePage.spec.slug}" 分支 3 款硬编码
  └─ 相关产品硬编码 3 卡 + CTA
src/partials/section-products.html
  └─ 3 卡 th:href 硬编码为 /product-vital 等固定 slug
```

## 模板设计

### theme.yaml

```yaml
customTemplates:
  page:
    - name: 产品详情
      file: page_product.html
```

### src/page_product.html 结构

```html
<include src="layout.html">
  <div th:with="slug=${singlePage.spec.slug}">
    <section th:if="${slug=='product-vital'}">...体征...</section>
    <section th:if="${slug=='product-diedao'}">...跌倒...</section>
    <section th:if="${slug=='product-sos'}">...紧急按钮...</section>
    <section th:unless="${slug=='product-vital' or slug=='product-diedao' or slug=='product-sos'}">
      ...通用占位...
    </section>
  </div>
</include>
```

每分支内含 7 块硬编码：面包屑、首图、标题、产品介绍卡片、规格表、相关产品、CTA。

### section-products.html

- 3 卡 `th:href` 硬编码：
  - 体征 `@{/product-vital}`
  - 跌倒 `@{/product-diedao}`
  - 紧急 `@{/product-sos}`

## 样式

- 复用 `src/css/main.css` 现有 `.product-specs`、`.product-highlight-card` 等，必要时在 `page_product.html` 内联
- 无新增 JS，纯静态

## 兼容与回滚

- 未创建对应 SinglePage 时访问 `/product-vital` 返回 Halo 404
- 回滚：`git revert` 移除 `page_product.html` 与 `theme.yaml` 条目，`section-products` 改回 `/archives`

## 风险

- 硬编码新增产品需改代码，接受该成本（产品少）
