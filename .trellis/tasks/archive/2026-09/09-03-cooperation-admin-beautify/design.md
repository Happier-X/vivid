# Design: 合作咨询后台美化

## 架构与边界
- 仅改 `plugin-cooperation/console/src/views/CooperationList.vue` 的 `template` 与 `<style scoped>`，不改 `api/cooperation.ts`、 `types/cooperation.ts`、后端 `CooperationController`、 `plugin.yaml`
- 依赖：`@halo-dev/components` 已由 `console-shared` 间接提供，`VCard/VButton/VInput/VSelect/VEmpty/VLoading/VModal/VBadge/VTag/VTable/VPagination` 均可用；若 `VDatePicker` 未暴露则用统一样式 `input[type=date]`
- 打包路径不变：`console/pnpm build` → `src/main/resources/console/main.js + style.css` → `gradle build` 打入 Jar

## 组件与布局
- 外层 `div.p-6 bg-[#eef7f7] min-h-[calc(100vh-64px)]` 保持与 Halo Pro 背景一致
- 筛选栏：`VCard` (`rounded-[12px] border-[rgba(217,236,233,0.9)] shadow-[0_8px_30px_rgba(36,109,116,0.06)] p-5`) 内 `grid grid-cols-5 gap-4 max-lg:grid-cols-3 max-md:grid-cols-2`
- 每个筛选项：`label.text-xs.font-medium.text-ink-700` + 控件 `h-9`，`VInput` 带 `Search` 图标，`VSelect` 带 `chevron`
- 按钮组：`ml-auto flex gap-2 self-end`，`VButton` 的 `type` 与 `loading` 状态
- 空状态：`VCard` 居中 `py-20`，图标 `div.h-16.w-16.rounded-full.bg-[#eef6f5].flex.items-center.justify-center` 含 `📋`，标题 `text-base font-black`，副标题 `text-sm text-gray-500`，`code` 样式地址
- 表格：外层 `VCard`，`VTable` 的 `thead` 覆写为 `bg-[#eef6f5]`，`tbody tr` 加 `transition-colors`
- 分页：`VCard` 单独包裹或表格底部 `border-t`，`VPagination` 的 `page` 与 `size` 双向绑定

## 视觉 Token
- 主色 #4fc7b7 / 悬停 #3db8a5 / 淡底 #eef6f5 / 页底 #eef7f7
- 文本 #1f2d38 / 次文本 #687783 / 边框 rgba(217,236,233,0.9)
- 圆角 12px，阴影 0_8px_30px rgba(36,109,116,0.06)，hover 0_18px_45px rgba(36,109,116,0.10)
- 字体：Halo 默认 `sans`，标题 `font-black tracking-tight`

## 数据流（不变）
```
VCard 筛选 → filters reactive → fetchList() → consoleApiClient.listCooperations → CooperationController.handleList → ListResult → VTable 渲染
```

## 兼容与回滚
- 仅样式变更，若美化后出现布局错乱，直接 `git checkout -- CooperationList.vue` 回退，无后端影响
- 构建产物 `console/main.js` 体积增量 < 10KB，Halo 加载无额外依赖
