# 彻底移除飞书残留依赖

## 目标

将上一任务 `08-31-cooperation-form-native` 遗留的所有飞书相关可选扩展、注释及文档提及彻底清理，实现全库 `grep -ri feishu` 零命中（除任务归档历史外），彻底与飞书解耦。

## 背景

- `08-31-cooperation-form-native` 已移除 `src/page_cooperation.html` 的 `iframe` 与 `my.feishu.cn` 业务依赖，主链路已去飞书化
- 残留：`worker/cooperation.ts` 底部注释的 `FEISHU_WEBHOOK_URL` 推送示例、`worker/README.md` 中“扩展：飞书 webhook”章节、`worker/wrangler.toml.example` 中 `FEISHU_WEBHOOK_URL` 变量及相关 `open.feishu.cn` 字样
- 用户明确要求：不要再依赖飞书，含可选扩展也不保留

## 需求

### R1 代码层清理

- `worker/cooperation.ts`：删除 `Env` 中 `FEISHU_WEBHOOK_URL` 字段及底部注释的飞书推送代码块，删除相关 `console` 注释中的飞书字样
- `worker/wrangler.toml.example`：删除 `FEISHU_WEBHOOK_URL` 配置项及注释

### R2 文档层清理

- `worker/README.md`：删除“扩展：飞书 webhook”整节及表格中 `FEISHU_WEBHOOK_URL` 行，将文档描述改为纯 Resend/SMTP 方案

### R3 校验

- 全库（`src/` + `worker/` + `templates/` + `README.md`）`grep -ri feishu` 无命中；`grep -ri "飞书"` 无命中（除归档任务历史可豁免）
- `pnpm check` 与 `pnpm build` 仍通过，表单提交主链路不受影响

## 非目标

- 不改动表单字段、校验、提交逻辑与样式
- 不新增替代推送通道（企微/钉钉等如需后续另开任务）

## 验收标准

- [ ] `grep -ri feishu C:/code/vivid --include="*.ts" --include="*.md" --include="*.toml" --exclude-dir=.trellis --exclude-dir=.git --exclude-dir=node_modules` 无输出
- [ ] `worker/cooperation.ts` 无 `FEISHU` 字样
- [ ] `worker/README.md` 无“飞书”二字
- [ ] `pnpm check` 与 `pnpm build` 通过
