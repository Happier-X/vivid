# 移除合作咨询后台时间筛选

## Goal
移除合作咨询后台筛选栏中的“开始时间 / 结束时间”日期筛选，简化操作，保持关键词、合作类型、处理状态三项核心筛选。

## Background
- 1.1.8 美化版筛选栏为 5 列（关键词/类型/状态/开始/结束），用户反馈时间筛选不常用且占用空间，希望移除
- 后端 `applyFilters` 的时间区间逻辑可保留，前端不再传参即可不生效

## Requirements
- 前端 `CooperationList.vue` 移除开始时间与结束时间的 `input[type=date]` 控件及对应 `label`
- 移除 `filters.startTime / filters.endTime` 响应式状态，`handleReset` 中不再重置该两项
- `fetchList` 的 `listCooperations` 调用不再传 `startTime/endTime`，`handleExport` 同步移除
- `watch` 中对时间变化的监听移除（当前仅监听 type/handled，已无时间监听，无需改动）
- 筛选栏 `grid` 由 5 列改为 3 列（关键词/类型/状态），按钮组保持 `ml-auto`
- 后端 `CooperationController` 的时间过滤可保留，无需改动

## Acceptance Criteria
- [ ] 后台筛选栏仅显示 关键词、合作类型、处理状态、搜索/重置/导出 3 按钮，无日期框
- [ ] 搜索/重置/导出功能与移除前一致，分页/详情/标记/删除均正常
- [ ] `pnpm build` 与 `gradle build` 均 SUCCESS，`console/main.js` 不再含 `startTime/endTime`
- [ ] 新 Jar 版本 1.1.9 可安装，旧时间参数即使手动传参后端仍兼容不报错

## Notes
- 轻量任务，PRD-only，直接实现
