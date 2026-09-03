# 合作咨询后台美化

## Goal
将合作咨询插件后台管理页从功能优先的初版升级为视觉精致的 Halo Pro 风格，对齐前台万椿微卡设计语言（12px 圆角、淡青阴影、主色 #4fc7b7），提升运营日常使用的愉悦度与专业感。

## Background
- 当前 `CooperationList.vue` 使用原生 `<input> / <select> / <table>`，筛选栏 5 控件挤一行，placeholder 灰度低，原生 date 样式与 Halo 不统一，按钮未用 `VButton` 主色，空状态仅文字
- 用户反馈“太丑”，截图显示筛选栏拥挤、空状态简陋
- 功能已打通（提交/列表/筛选/删除/导出/标记），需在不改动接口契约前提下纯视觉重构

## Requirements

### 1. 筛选栏重构
- 容器：`VCard` 包裹，`12px` 圆角，`border-[rgba(217,236,233,0.9)]` + `shadow-[0_8px_30px_rgba(36,109,116,0.06)]`，内边距 `p-5`
- 控件：关键词 `VInput`（带搜索图标）、合作类型 `VSelect`、处理状态 `VSelect`（选项带 `VBadge` 彩色）、开始/结束时间 `VDatePicker`（或 `VInput type=date` 但统一样式）
- 布局：响应式 `grid`（`lg:5列` → `md:3列` → `sm:2列`），标签 `text-xs font-medium text-ink-700`，控件高度统一 `h-9`
- 按钮：搜索 `VButton type=primary` 主色 #4fc7b7，重置 `VButton secondary`，导出 `VButton` 带图标，`total===0` 时 `disabled` 且置灰

### 2. 空状态与加载态美化
- 加载中：居中 `VLoading` + “正在加载合作咨询…” 文案，`py-20`
- 空状态：`VEmpty` 风格插画（或 `📋` 放大至 `text-5xl` + 淡青圆底），标题 `暂无合作咨询数据` 加粗，副标题双行，底部 `VButton` 引导“去查看表单”或“调整筛选条件”，显示 `表单提交地址` 为 `code` 样式

### 3. 表格与分页美化
- 表格：`VTable` 的 `striped` 与 `hover`，表头 `bg-[#eef6f5] text-[#246d74] font-bold text-xs`，行高 `py-3`，公司名称 `font-medium` + `truncate` + `title`，意向 `max-w-[240px] truncate`
- 标签：合作类型 `VTag` 淡青底，处理状态 `VBadge`（未处理 amber / 已处理 green），带圆点
- 操作列：`VButton size=xs` 图标化（查看 `EyeIcon`、标记 `CheckIcon`、删除 `TrashIcon`），悬停动效
- 分页：`VPagination` 居中，`每页` 选择器与 `共 N 条` 在同一行，`bg-white` 圆角卡片

### 4. 详情与删除弹窗美化
- 详情抽屉：宽度 `560px`，头部 `sticky` 带 `VBadge`，内容 `VCard` 分组，字段 `label w-28 text-gray-500` + `value`，消息用 `prose` 样式，底部按钮主色
- 删除确认：`VModal` 带红色图标，标题加粗，描述中公司名高亮

### 5. 视觉一致性
- 主色 #4fc7b7、辅色 #eef6f5 / #eef7f7、文本 #1f2d38 / #687783、边框 rgba(217,236,233,0.9)，与前台一致
- 圆角统一 12px，阴影 0_8px_30px rgba(36,109,116,0.06)，hover 0_18px_45px rgba(36,109,116,0.10)
- 不改动任何接口、数据流、权限逻辑，仅替换 `template` 与 `scoped style`，复用现有 `api/cooperation.ts`

## Out of Scope
- 新增筛选字段或接口变更
- 表格列的增删或数据源变更
- 深色模式适配（首版仅浅色）
- 动画库引入（仅用 Tailwind transition）

## Acceptance Criteria
- [ ] 筛选栏在 1280px/768px/375px 下均无拥挤换行异常，控件对齐，VSelect/VDatePicker 样式与 Halo Pro 一致
- [ ] 搜索/重置/导出按钮主色与前台一致，导出 `total===0` 时置灰且不可点
- [ ] 空状态插画居中，文案与引导按钮完整，表单地址为 code 样式
- [ ] 表格表头淡青底、行 hover 高亮、标签彩色、操作图标化，分页居中
- [ ] 详情抽屉与删除弹窗视觉精致，字段分组清晰
- [ ] 功能回归：筛选/分页/详情/标记/删除/导出均与美化前一致，`pnpm build` 与 `gradle build` 均 SUCCESS，`console/main.js` 含新样式

## Notes
- 仅改 `console/src/views/CooperationList.vue` 的 template/style，`console/src/api/cooperation.ts` 与 `types` 不动
- 若 `@halo-dev/components` 的 `VDatePicker` 引入成本高，可暂用统一样式的原生 `input[type=date]` + Tailwind 美化
