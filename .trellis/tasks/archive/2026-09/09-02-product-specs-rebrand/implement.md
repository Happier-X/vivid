# 实施计划：产品详情厂家资料脱敏重制

## 前置

- [ ] 本地确认 `智慧养老厂家资料` 中 6 份核心文档可读
- [ ] 确认 `src/page_product.html` 3 分支结构

## 阶段 1：资料解析

- [ ] 读取 `毫米波睡眠监测仪 X1产品参数介绍.pdf` 与 `X1介绍材料.pdf`，提取 X1/X2 公共参数
- [ ] 读取 `跌倒监测传感器L4P-产品手册V3.2.pdf` 与 `L4P产品参数介绍.pdf`，提取 L4P 参数
- [ ] 读取 `SOS-C06产品参数控标点.docx` 与 `说明书.pdf`，提取 SOS-C06 参数
- [ ] 记录原始参数到本地笔记（不提交），标记需脱敏字段

## 阶段 2：脱敏与模板更新

- [ ] `src/page_product.html` 中 `vital-monitor` 分支的 4 卡片产品介绍与 8 行规格表按 X1/X2 脱敏后替换
- [ ] 同步替换 `fall-monitor`（L4P）与 `emergency-button`（SOS-C06）两分支
- [ ] 确保无 `希卡立|驰通达|HECARAY|X1|X2|L4P|SOS-C06` 残留
- [ ] 验证：`grep -ri "希卡立\|驰通达" src/page_product.html` 无命中

## 阶段 3：忽略与构建

- [ ] `.gitignore` 追加 `智慧养老厂家资料/`（已完成，需验证）
- [ ] `pnpm check` 与 `pnpm build` 通过，`templates/page_product.html` 含新参数

## 验证命令

```bash
grep -ri "希卡立\|驰通达\|HECARAY" src/ || echo "no brand"
grep -ri "X1\|X2\|L4P\|SOS-C06" src/page_product.html || echo "no model"
pnpm check
pnpm build
```

## 风险文件与回滚

- 高风险：`src/page_product.html`（大段硬编码替换）
- 回滚：`git revert` 到上一版本

## 提交前检查

- [ ] 厂家资料未提交（`.gitignore` 生效）
- [ ] 3 款产品参数与厂家 PDF 数值一致但无品牌
