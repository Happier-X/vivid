# 企业官网风格 Halo 主题开发（参考 mikapu.cn）

## 目标

将现有的 theme-vite-starter 改造为一个**通用企业官网风格的 Halo 主题**（可复用、可发布），视觉与布局参考 https://mikapu.cn/。首页各区块内容全部通过 settings.yaml 后台表单配置驱动。

## 背景与已确认事实

- 项目 `C:/code/vivid` 是基于 `@halo-dev/vite-plugin-halo-theme` 的 Vite 主题模板（theme-vite-starter）
- 现有模板：index、post、page、archives、categories、category、tag、tags（位于 `src/`）
- 布局复用采用 include/template 语法（`src/partials/layout.html`，Vite 构建期处理，与 Thymeleaf 运行时语法完全隔离）
- 样式目前为原生 CSS（`src/css/main.css`），JS 为 TypeScript，构建命令 `pnpm build`（tsc + vp build + theme-package）
- settings.yaml 目前只有一个样式分组（背景颜色）
- 参考站 mikapu.cn 本身即 Halo 站点，验证了该风格用 Halo 主题实现的可行性

## 参考站风格要点（mikapu.cn）

1. Hero 首屏：标签 + 大标题 + 副标题 + 双 CTA 按钮 + 特性标签组 + 产品大图
2. 核心科技：编号卡片式特性展示（01-04），图文结合
3. 精选产品：商品卡片网格 + "查看全部"链接
4. 功能介绍区：左文右图（APP 智能互联等卖点）
5. CTA 横幅：渐变背景行动号召
6. 数据统计栏：数字亮点（500万+ / 30+ / 99.9% 等）

## 需求

### R1 首页企业官网区块布局

首页按上述 6 类区块组织，每个区块：

- 支持后台开关显隐（switch）
- 文案、图片（attachment）、链接均可配置
- 多条目内容（特性项、功能点、统计项）用 FormKit `array` 类型配置

### R2 页面模板覆盖

| 页面                | 实现方式                                     |
| ------------------- | -------------------------------------------- |
| 首页                | `index.html` 区块化布局                      |
| 产品列表 / 新闻列表 | 复用分类模板（如分类"产品中心"、"新闻资讯"） |
| 文章详情            | `post.html`                                  |
| 关于我们 / 联系我们 | `page.html`（自定义页面）                    |
| 归档/分类索引/标签  | 现有辅助模板统一新视觉                       |

导航通过 Halo 菜单系统（menuFinder.getPrimary()）组装。

### R3 内容建模

纯展示型官网，无商城功能。产品与新闻均用「文章 + 分类」承载；首页精选产品区通过 settings 的 `categorySelect` 选择来源分类，用 postFinder 查询渲染。

### R4 样式方案

TailwindCSS 4（`@tailwindcss/vite` 插件 + CSS 中 `@import "tailwindcss"`）；主题色等设计令牌用 CSS 变量定义，供后台配色设置映射。

## 已决事项

- [x] 主题定位：通用企业官网主题，可复用；首页区块全部后台配置驱动
- [x] 页面范围：见 R2
- [x] 内容建模：文章+分类，无商城功能
- [x] 样式方案：TailwindCSS 4
- [x] 首页区块可配置程度：全配置驱动 + 区块显隐开关

## 验收标准

1. `pnpm build` 构建成功，产物包含 templates/ 下全部页面模板与 assets 静态资源
2. `pnpm check`（vp check --fix）无错误
3. 安装主题后，后台主题设置页可见全部配置分组（Hero/核心特性/精选产品/功能介绍/CTA横幅/数据统计/页脚/样式），且每项修改后前台正确生效
4. 关闭任一首页区块开关后，该区块在前台消失且布局不错乱
5. 首页精选产品区显示所选分类下的最新 N 篇文章（N 可配置）；未选分类时回退为最新文章
6. 导航菜单由 Halo 主菜单驱动，移动端有可用汉堡菜单
7. 全部页面模板在桌面端与移动端下布局正常（响应式）
8. 文章详情、自定义页面、归档、标签、分类等页面与新视觉风格一致

## 范围外（Out of Scope）

- 商城能力（价格、购物车、支付、SKU）
- 商品插件集成或自定义商品模型
- 多语言/i18n
- 暗色模式（首期不做，样式令牌预留扩展）
- 博客评论样式深度定制（保留 halo:comment 默认挂载点即可）
