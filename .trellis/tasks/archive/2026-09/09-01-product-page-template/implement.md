# 实施计划：产品详情改为页面模板复用

## 前置

- [ ] 确认 `src/page_about.html` 等 SinglePage 模板结构与 `singlePage.spec.slug` 可用性

## 阶段 1：模板注册与创建

- [ ] `theme.yaml` 新增 `customTemplates.page` 条目 `产品详情 -> page_product.html`
- [ ] 新建 `src/page_product.html`，按 design.md 8 块硬编码 3 款产品分支
- [ ] 验证：`grep -n "page_product" theme.yaml` 命中

## 阶段 2：首页链路

- [ ] `src/partials/section-products.html` 将 3 卡 `th:href` 改为硬编码 `/product-vital` 等
- [ ] 验证：`grep -n "product-vital" src/partials/section-products.html` 命中 3 处

## 阶段 3：样式与构建

- [ ] 如需，`src/css/main.css` 补充产品详情表格样式
- [ ] 验证：`pnpm check` 通过

## 阶段 4：整体验收

- [ ] `pnpm build` 通过，产物 `templates/page_product.html` 存在
- [ ] 后台新建 3 个 SinglePage，Slug 分别为 `product-vital`/`product-diedao`/`product-sos`，选用“产品详情”模板，访问对应路径即见硬编码详情
- [ ] 首页精选点击链路正确

## 验证命令

```bash
pnpm check
pnpm build
ls templates/page_product.html
grep -rn "product-vital" templates/
```

## 风险文件与回滚

- 高风险：`theme.yaml`、`src/page_product.html`、`src/partials/section-products.html`
- 回滚：`git revert` 到上一检查点

## 提交前检查

- [ ] 无 `postFinder` 残留，无 NPE
- [ ] 3 款产品硬编码文案与图库路径正确
