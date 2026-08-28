# 图片真实化-占位图替换

## Goal

将站点中所有渐变/SVG 占位图替换为公司资料中的真实图片，提升上线即用度，保留 mikapu 版式质感。

## Requirements

- 扫描 `山东万椿智能科技有限公司资料` 内 5 个 PPTX，提取 `ppt/media/*` 真实产品/场景/平台截图
- 归档到 `public/assets/images/wanchun/` 并按语义命名（hero、product-_、platform、solution-_ 等），同时提供 `src/assets/images` 兼容
- 替换占位：`section-hero` 右侧主图、`section-features`、`section-intro`、`section-products` 4 卡、`page_solutions` 三场景、`page_about` 证书/社交保持 SVG，产品类占位优先替换
- 图片需压缩/适配（webp 兼容，保持 <300KB/张），使用 `th:src` + `src` 双写，`alt` 完整
- 未找到对应实图时保留渐变占位并添加注释

## Acceptance Criteria

- [ ] `public/assets/images/wanchun/` 包含 ≥8 张已命名实图
- [ ] 上述模板中无大面积渐变占位，`grep -rn "from-primary to-primary"` 显著减少
- [ ] `pnpm build && pnpm check` 通过，页面可视正常
- [ ] 图片加载路径在 Halo 构建后可访问

## Notes

- PPTX 解压后筛选 >30KB 的有效图，优先选择产品白底与场景实拍
