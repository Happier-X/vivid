# Implement: Halo合作咨询插件后台管理页

## 执行清单（按序）

### 阶段 0：准备与脚手架

- [ ] 0.1 检查 Halo 版本与插件约束：确认 `halo.version=2.20.11`, Java 17, `run.halo.plugin.devtools:0.6.2`
- [ ] 0.2 在 `plugin-cooperation/` 下创建 `console/` 脚手架（参考 `create-halo-plugin` + `plugin-todolist`）：`package.json`, `vite.config.ts`（`uiPlugin` from `@halo-dev/ui-plugin-bundler-kit`）, `src/index.ts`, `src/views/CooperationList.vue`
- [ ] 0.3 更新 `plugin-cooperation/build.gradle`：新增 `console` 构建钩子或文档化手动 `pnpm build` 步骤

### 阶段 1：后端 Extension 与 API

- [ ] 1.1 修改 `extension/Cooperation.java`：新增 `Boolean handled = false`，更新 Schema 注解，兼容 null→false
- [ ] 1.2 扩展 `controller/CooperationController.java`：
  - [ ] 1.2.1 `handleList` 新增查询参数 `keyword, type, handled`，实现内存过滤（company/contact/phone 模糊, type 单值, handled 匹配）后分页
  - [ ] 1.2.2 新增 `handleDelete`：`DELETE /.../cooperations/{name}`，`requireAdmin` 校验后 `extensionClient.delete`
  - [ ] 1.2.3 新增 `handleUpdateHandled`：`PUT /.../cooperations/{name}/handled`，Body `handled` 布尔，`extensionClient.update`
  - [ ] 1.2.4 新增 `handleExport`：`GET /.../cooperations/export`，同样过滤后生成 CSV（UTF-8 BOM，`Content-Disposition: attachment`）
- [ ] 1.3 更新 `config/SecurityConfig.java`：放行 `POST` 匿名，其余需 `ROLE_ADMIN`，新增 DELETE/PUT 规则
- [ ] 1.4 （可选）新增 `src/main/resources/extensions/roleTemplate.yaml` 定义 `cooperation:view/manage` 权限

### 阶段 2：Console 前端

- [ ] 2.1 实现 `console/src/index.ts`：`definePlugin` 注册路由 `/cooperations` 与菜单 `console:menu`
- [ ] 2.2 实现 `CooperationList.vue`：
  - 顶部：关键词搜索框 + 合作类型单选 + 处理状态单选 + 搜索/重置 + 导出按钮
  - 表格：`VTable` 展示 8 列，截断 message，handled 标签，操作列（查看/删除/标记已处理）
  - 分页：`VPagination` 绑定 `page/size`（10/20/50）
  - 详情抽屉：`VModal` 展示全字段，支持切换 handled
  - 删除：`VModal` 二次确认
  - 空状态与 Toast
- [ ] 2.3 封装 API 客户端：`console/src/api/cooperation.ts` 调用 `consoleApiClient` 封装上述 5 个接口
- [ ] 2.4 构建验证：`cd console && pnpm install && pnpm build`，确认输出至 `src/main/resources/console/main.js` + `style.css`

### 阶段 3：插件描述与文档

- [ ] 3.1 更新 `src/main/resources/plugin.yaml`：新增 `spec.console` 或确认 `customACP` 注册，版本升至 `1.1.0`
- [ ] 3.2 更新 `plugin-cooperation/README.md` 与 `docs/DEPLOY.md`：标注 `server/cooperation` Deprecated，增加 Console 使用说明与截图指引
- [ ] 3.3 更新根 `README.md`（如有）与 `settings.yaml` 注释

### 阶段 4：构建验证

- [ ] 4.1 执行 `pnpm build`（主题）确保未破坏主题构建
- [ ] 4.2 执行 `gradle :plugin-cooperation:build -x test`，检查 `jar tf` 含 `console/main.js`
- [ ] 4.3 本地 Halo `dev` 模式联调：提交表单 → Console 列表可见 → 筛选/分页/详情/删除/导出/标记已处理 全流程
- [ ] 4.4 接口压测：401/403 鉴权、429 限流、蜜罐静默

## 验证命令

```bash
# Console 构建
cd plugin-cooperation/console && pnpm build

# 插件构建
./gradlew :plugin-cooperation:build -x test
jar tf plugin-cooperation/build/libs/cooperation-plugin-*.jar | grep console

# 主题构建（回归）
pnpm build

# Halo 联调（需本地 Halo）
curl -X POST http://localhost:8090/apis/api.cooperation.vivid.run/v1alpha1/cooperations -H "Content-Type: application/json" -d '{"company":"测试公司","contact":"张三","phone":"13812345678","type":"institution","typeLabel":"养老机构合作","message":"测试","website":""}'
curl http://localhost:8090/apis/api.cooperation.vivid.run/v1alpha1/cooperations?page=0&size=20 -H "Cookie: halo-SESSION=xxx"
```

## 风险文件与回滚点

- 风险文件：`Cooperation.java`（Extension 字段变更）、`CooperationController.java`（新增3接口）、`SecurityConfig.java`（权限）、`plugin.yaml`（Console 注册）、`console/**`（前端）
- 回滚点：阶段1前备份旧 `Cooperation.java` 与 Jar；阶段2前备份 `plugin.yaml`；任何阶段失败可 `git stash` 回退并重新 `gradle build`
- 阻塞：若 Halo 2.20 的 `ui-plugin-bundler-kit` 版本不匹配，需锁定 `@halo-dev/ui` 版本与官方 `plugin-todolist` 对齐

## 验收前检查

- [ ] `prd.md` 验收项逐项勾选
- [ ] 1Panel 部署手册已更新废弃声明
- [ ] `implement.jsonl` / `check.jsonl` 已填充真实条目
