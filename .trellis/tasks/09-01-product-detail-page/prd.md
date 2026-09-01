# 产品详情页开发

## 目标

为首页“精选产品”3 卡（体征监测仪 / 跌倒监测仪 / 紧急按钮）提供点击后的单品详情页，采用标准化产品模板（图库/卖点/规格表/相关推荐/CTA 统一），打通 `section-products` 点击链路，支持后台以文章内容约定方式维护，提升转化与复用。

## 背景与已确认事实

- 技术栈：Halo 主题 Vite+Thymeleaf 双层模板；产品以文章+分类承载（无独立商品模型），`src/post.html` 为通用文章详情（封面+正文+标签+上下篇+评论）
- 现状：`src/partials/section-products.html` 3 卡均 `th:href="@{/archives}"` 跳列表，非单品详情；`post.html` 无产品专属区块
- 已有资产：`src/assets/images/wanchun/product-*.jpg/png`（体征/跌倒/紧急按钮）与文案已定版
- 已决策：
  - Q1=B 标准化产品模板（非极简富文本）
  - Q2=内容约定式（`spec.cover` 为主图，`content` 中 `## 规格参数` 表格与 `## 图库` 图片组为结构化数据，未按约定回退富文本），零插件
  - Q3=新建“产品中心”分类（`product-center`），首页精选与详情相关推荐均定向该分类，按钮推荐样式

## 需求

### R1 详情页版式（post.html 扩展）

- 基于 `src/post.html` 扩展为产品感知版式，固定区块顺序：
  1. 面包屑（首页 / 产品中心 / 当前产品）+ 返回
  2. 首图/图库轮播（`spec.cover` 主图 + `## 图库` 附件图，JS 轻量轮播，无第三方库）
  3. 标题 + 发布信息 + 标签
  4. 核心卖点清单（正文 `## 核心卖点` 的 `ul` 提取并卡片化）
  5. 规格参数表（`## 规格参数` 的 Markdown 表格，主题统一样式 `border-line` 表格）
  6. 正文其余富文本（`rich-content`）
  7. 相关产品推荐（`postFinder.list({categoryName:"产品中心", size:3})` 排除当前篇，按按钮推荐样式或 3 卡网格）
  8. 合作 CTA（跳转 `/cooperation`）

### R2 链接打通

- `section-products.html` 3 卡由静态 `/archives` 改为按 `postFinder` 查询“产品中心”最新 3 篇的 `status.permalink` 动态链接，`th:href` 与 `th:src`/`th:text` 动态渲染；无数据时回退静态占位

### R3 数据维护约定

- 作者新建文章，分类选“产品中心”，封面设为产品主图，正文中按约定书写：

  ```markdown
  ## 核心卖点

  - 卖点1
  - 卖点2

  ## 规格参数

  | 参数     | 值  |
  | -------- | --- |
  | 监测距离 | 5m  |

  ## 图库

  ![图1](/upload/xxx.jpg)
  ```

- 模板通过 Thymeleaf 对 `post.content.content` 的 HTML 做了简易字符串/正则抽取，若未按约定则整块隐藏，避免残留空区块

## 非目标

- 商城交易链路（下单/支付/库存/购物车）
- 独立商品数据库或 Halo 插件扩展
- 复杂图库后台字段（首期仅 `cover` + 内容图组）

## 验收标准

- [ ] 1. 点击首页精选产品任意一卡，进入对应单品详情页，URL 为该文章 `permalink`，非 `/archives`
- [ ] 2. 详情页含面包屑（首页/产品中心/标题）、首图、按约定提取的卖点/规格表/图库轮播，未按约定时对应区块隐藏
- [ ] 3. `section-products.html` 3 卡为动态数据（标题/封面/摘要来自“产品中心”分类最新 3 篇），无该分类时回退静态
- [ ] 4. 详情页底部含相关产品 3 卡（同分类排除当前）与合作 CTA（指向 `/cooperation`）
- [ ] 5. `pnpm check` 与 `pnpm build` 通过，产物 `templates/post.html` 与 `templates/index.html` 含新逻辑
- [ ] 6. 无该分类或空数据时页面无残留空区块与 NPE

## 约束

- 遵循 `.trellis/spec/frontend/halo-theme.md`：`theme.config` 契约、Finder API 参数需在线核对、空数据安全导航
- 保持 `post.html` 对非产品文章的兼容（非“产品中心”分类时不显示产品专属区块）
