# 技术设计：企业官网风格 Halo 主题

## 架构总览

沿用现有 Vite + `@halo-dev/vite-plugin-halo-theme` 架构，不改变构建体系。核心变化：

1. **样式层**：引入 TailwindCSS 4（`@tailwindcss/vite` 插件），`src/css/main.css` 改为 `@import "tailwindcss"` + 设计令牌定义
2. **模板层**：首页拆分为区块 partials，全部数据取自 `theme.config`
3. **配置层**：settings.yaml 扩展为多分组表单，驱动首页内容

## 数据流与契约

```
settings.yaml 表单定义
   → Halo Console 渲染后台设置页
   → 存入 ConfigMap
   → 模板中 theme.config.<group>.<name> 读取
   → Thymeleaf 渲染 + postFinder/menuFinder 查询动态内容
```

### settings.yaml 分组设计

| 分组 group | 内容                                                                                       | 关键字段类型                    |
| ---------- | ------------------------------------------------------------------------------------------ | ------------------------------- |
| `style`    | 主色、辅助色、圆角风格                                                                     | color / radio                   |
| `hero`     | 显隐开关、顶部标签、标题、副标题、CTA×2（文字+链接）、特性标签（list）、主图（attachment） | switch/text/url/list/attachment |
| `features` | 显隐开关、小标题、大标题、配图、条目数组                                                   | array: {title, description}     |
| `products` | 显隐开关、小标题、大标题、来源分类（categorySelect）、数量、查看全部开关                   | switch/categorySelect/number    |
| `intro`    | 显隐开关、小标题、大标题、描述、功能点数组、配图                                           | array: {title, description}     |
| `cta`      | 显隐开关、标题、描述、按钮×2                                                               | switch/text/url                 |
| `stats`    | 显隐开关、统计项数组                                                                       | array: {value, label}           |
| `footer`   | 版权文本、备案号、附加链接数组                                                             | text/list                       |

约定：所有数组条目用 FormKit `array`（对象数组）或 `list`（字符串数组）；图片一律 `attachment`；布尔开关一律 `switch`。

### 设计令牌方案

- `src/css/main.css` 中以 `@theme` 定义 Tailwind 4 主题变量（颜色、字体、圆角）
- 运行时覆盖：layout.html `<head>` 内联 `<style th:inline="css">` 或 `:root` style 属性，把 `theme.config.style.*` 写入 CSS 变量（如 `--color-primary`），Tailwind 类引用同一变量 → 后台改色即时生效
- 区块通用 class：容器宽度、区块间距、小标题/大标题排版抽为公共模式，在各 partial 中复用

## 模板结构

```
src/
├── index.html                  # 首页：按序 include 各区块 partial
├── post.html                   # 文章详情（产品/新闻共用）
├── page.html                   # 自定义页面（关于我们/联系我们）
├── archives.html               # 归档
├── categories.html / category.html
├── tags.html / tag.html
└── partials/
    ├── layout.html             # 全局布局：header(logo+菜单) + slot + footer(halo:footer)
    ├── header.html             # 顶部导航（menuFinder.getPrimary()，移动端汉堡）
    ├── footer.html             # 页脚（版权/备案号/链接 + halo:footer）
    ├── section-hero.html       # Hero 首屏
    ├── section-features.html   # 编号卡片特性区
    ├── section-products.html   # 精选产品网格（postFinder.list 按 category 查询）
    ├── section-intro.html      # 左文右图功能介绍
    ├── section-cta.html        # CTA 横幅
    ├── section-stats.html      # 数据统计栏
    └── post-card.html          # 文章卡片（产品/新闻列表与首页精选共用）
```

### 关键实现点

1. **区块显隐**：index.html 中每个 include 外包 `th:block th:if="${theme.config.hero.enabled}"`（switch 值为 boolean）
2. **精选产品查询**：
   ```html
   <th:block
     th:with="
     productPosts = ${theme.config.products.category != null && theme.config.products.category != '' 
       ? postFinder.list({categoryName: theme.config.products.category, size: theme.config.products.count}) 
       : postFinder.list({size: theme.config.products.count})}"
   ></th:block>
   ```
   （实现时须核对 postFinder 实际参数名，以在线文档为准，不凭记忆写死）
3. **导航**：header 用 `th:with="menu = ${menuFinder.getPrimary()}"` 遍历 menuItems；移动端菜单显隐由少量 TS 控制
4. **静态资源路径**：partials 内引用资源一律相对 `src/` 书写（如 `./css/main.css`）
5. **theme.yaml 更新**：displayName/description/logo 等元信息改为企业官网主题定位；`settingName`/`configMapName` 与 settings.yaml 保持一致

## JS 层（保持最小）

- `src/js/main.ts`：移动端菜单开合、必要的滚动交互（如导航吸顶阴影）
- 不引入框架，保持原生 TypeScript

## 兼容性与迁移

- 现有 starter 的 post-list 样式将被替换；post-card.html 重写为企业风格卡片
- settings.yaml 原 `background_color` 字段移除，统一进 `style` 分组（无线上数据迁移负担，属首次改造）
- `templates/` 为构建产物目录（gitignore），不影响版本管理

## 回滚考虑

单仓库单分支顺序提交，按 implement.md 的检查点分批 commit；任一阶段出问题可 revert 到上一检查点。
