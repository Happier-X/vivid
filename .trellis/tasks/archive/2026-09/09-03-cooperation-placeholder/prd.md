# 合作意向表单 placeholder 统一

## Goal
将合作意向提交表单的所有输入框 placeholder 统一为“请输入XX”简洁格式，提升表单一致性与专业度。

## Requirements
- 公司名称输入框 placeholder 改为 `请输入公司名称`
- 联系人输入框 placeholder 改为 `请输入联系人`
- 联系电话输入框 placeholder 改为 `请输入联系电话`（原为带示例的长文案）
- 合作意向说明（textarea）placeholder 改为 `请输入合作意向说明`
- 修改源文件 `src/page_cooperation.html`，并同步构建产物 `templates/page_cooperation.html`
- 保持其它逻辑、校验、样式不变

## Acceptance Criteria
- [ ] `src/page_cooperation.html` 中 4 个字段 placeholder 已按上述映射更新
- [ ] `templates/page_cooperation.html` 同步更新
- [ ] 本地构建或预览无报错，表单正常显示
- [ ] 无其它 placeholder 残留旧文案

## Notes
- 轻量任务，PRD-only，无需 design.md / implement.md
