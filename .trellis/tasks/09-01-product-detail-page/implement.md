# 实施计划：产品详情页开发

## 前置

- [ ] 阅读 `.trellis/spec/frontend/halo-theme.md` 与 `references/finder-apis.md`（核对 `postFinder.list` 参数与 `PostVo` 字段）
- [ ] 确认 `src/post.html` 与 `src/partials/section-products.html` 当前结构

## 阶段 1：详情页 `post.html` 产品分支

- [ ] 在 `post.html` 头部加 `th:with="isProduct=..."` 判断
- [ ] 首图/图库区块：`isProduct` 时渲染，`spec.cover` 主图 + `content` 中 `## 图库` 图片横滑（首期可直接渲染 `post.content.content` 由 CSS 美化，图库轮播 JS 可选）
- [ ] 卖点/规格表区块：`th:if="${#strings.contains(post.content.content, '规格参数')}"` 包裹，表格样式在 `src/css/main.css` 新增 `.product-specs`
- [ ] 相关产品：`th:with="related=..."` 查询“产品中心” 4 篇过滤当前取 3，`th:each` 渲染，空时隐藏
- [ ] 合作 CTA：`a th:href="@{/cooperation}"`
- [ ] 验证：`grep -n "isProduct" src/post.html` 有定义，非产品文章不显示产品区块

## 阶段 2：首页精选动态化

- [ ] `section-products.html` 将静态 3 卡改为 `th:with="productPosts=..."` 动态，`th:each` 渲染，字段 `spec.cover/title` `status.excerpt/permalink`
- [ ] 保留静态 3 卡作为 `th:if="${#lists.isEmpty(productPosts.items)}"` 回退
- [ ] `th:href` 由 `"/archives"` 改为 `${post.status.permalink}`
- [ ] 验证：无分类时回退静态，有分类时动态

## 阶段 3：样式与脚本（按需）

- [ ] `src/css/main.css` 新增 `.product-specs table` 与图库缩略图样式
- [ ] 如需轮播，新增 `src/js/product-gallery.ts` 并在 `src/js/main.ts` 引入（首期可选）
- [ ] 验证：`pnpm check` 无新增 lint 报错

## 阶段 4：整体验收

- [ ] `pnpm check` 通过
- [ ] `pnpm build` 通过，产物 `templates/post.html` 含 `isProduct` 分支与 `postFinder.list`，`templates/index.html` 含动态精选
- [ ] 手动：创建“产品中心”分类与 3 篇产品文章，验证首页点击链路、详情卖点/规格/相关推荐、非产品文章保持原样

## 验证命令

```bash
pnpm check
pnpm build
grep -rn "isProduct\|product-center\|产品中心" templates/ | head
```

## 风险文件与回滚

- 高风险：`src/post.html`（产品分支）、`src/partials/section-products.html`（动态化）、`src/css/main.css`
- 回滚点：每阶段单独 commit，`git revert` 到上一检查点

## 提交前检查

- [ ] `postFinder.list` 参数名已在线核对
- [ ] 空数据与非产品分类有安全导航与隐藏逻辑
- [ ] 无硬编码产品文案残留
