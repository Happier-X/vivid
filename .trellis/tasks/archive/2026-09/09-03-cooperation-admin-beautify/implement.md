# Implement: 合作咨询后台美化

## 执行清单

### 阶段 1：筛选栏
- [ ] 1.1 外层 `div` 改为 `VCard`，`grid` 响应式，`gap-4`，`p-5`，`rounded-[12px]`
- [ ] 1.2 关键词 `VInput` 加 `prefix-icon`，`placeholder` 保持，`@keyup.enter` 保留
- [ ] 1.3 合作类型/处理状态 `VSelect` 替换原生 `select`，`options` 来自 `TYPE_OPTIONS/HANDLED_OPTIONS`，`VBadge` 在选项内预览
- [ ] 1.4 开始/结束时间 `VDatePicker` 或统一样式 `input[type=date]`，`h-9`，`border` 统一
- [ ] 1.5 按钮组 `VButton`：搜索 `type=primary`，重置 `secondary`，导出带 `Download` 图标，`disabled` 逻辑保留

### 阶段 2：空状态与加载
- [ ] 2.1 加载中 `VLoading` 居中 `py-20`
- [ ] 2.2 空状态 `VEmpty` 风格：图标圆底淡青，标题/副标题，`code` 样式地址，`重置` 引导按钮

### 阶段 3：表格与分页
- [ ] 3.1 表头 `bg-[#eef6f5] text-[#246d74]`，行 `hover:bg-gray-50 transition`，`max-w` 截断保留 `title`
- [ ] 3.2 标签 `VTag`/`VBadge` 彩色，`typeLabel` 淡青，`handled` 绿/amber
- [ ] 3.3 操作列 `VButton size=xs` 图标化，`@click.stop` 保留
- [ ] 3.4 分页 `VPagination` 外层 `VCard` 或表格底部，`page/pageSize` 绑定与 `handleSizeChange/goPage` 保留

### 阶段 4：详情与删除
- [ ] 4.1 详情抽屉 `w-[560px]`，头部 `sticky`，`VBadge` 与 `VTag` 在头部，字段 `label w-28` 布局保留
- [ ] 4.2 删除确认 `VModal` 带红色图标，按钮 `VButton` 样式

### 阶段 5：构建验证
- [ ] 5.1 `cd plugin-cooperation/console && pnpm build` → `src/main/resources/console/main.js` 更新
- [ ] 5.2 `./gradlew :plugin-cooperation:build -x test` → `jar tf` 含新 `console`，`pnpm build` 主题回归
- [ ] 5.3 本地 Halo 截图对比：1280/768/375 三断点无拥挤，功能（筛选/分页/详情/标记/删除/导出）回归

## 验证命令
```bash
cd plugin-cooperation/console && pnpm build
./gradlew :plugin-cooperation:build -x test
pnpm build
```

## 风险与回滚
- 风险文件：仅 `CooperationList.vue`，无后端风险
- 回滚：`git checkout -- plugin-cooperation/console/src/views/CooperationList.vue && pnpm build && gradle build`
