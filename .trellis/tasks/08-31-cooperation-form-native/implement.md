# 实施计划：合作咨询表单去飞书化-自建原生表单对接

## 前置

- [ ] 阅读 `.trellis/spec/frontend/halo-theme.md`（settings ↔ theme.config 契约、双层模板隔离）
- [ ] 确认现有 `src/page_cooperation.html` 注释内表单结构与样式类

## 阶段 1：配置层

- [ ] `settings.yaml` 新增分组 `cooperation`（endpoint / receiver_email / success_title / success_desc），提供默认值与 help
- [ ] `pnpm check` 验证 YAML 可解析，后台设置页可渲染（本地无 Halo 实例时至少保证构建不报错）

## 阶段 2：模板改造

- [ ] `src/page_cooperation.html` 删除 `iframe` 块及仅服务于 iframe 的外层容器
- [ ] 启用并增强原生表单：`id="cooperation-form-root"` 注入 `data-endpoint/receiver-email/success-*`，表单内补齐 `data-error-for` 占位、蜜罐 `website`、按钮 `data-role`、成功/失败/未配置三态容器
- [ ] 保持右侧三横卡与 Why 卡不动，确认响应式 `lg:grid-cols-2` 布局
- [ ] 验证：`grep -rn "feishu\|iframe" src/` 在合作页无命中

## 阶段 3：前端交互

- [ ] 新建 `src/js/cooperation-form.ts` 实现 `initCooperationForm`（校验、蜜罐、限重提交、fetch、成功/失败态切换）
- [ ] `src/js/main.ts` 引入并调用 `initCooperationForm()`（幂等、无 DOM 时 return）
- [ ] 处理 `endpoint` 为空分支：显示未配置提示 + mailto 备用
- [ ] 验证：`pnpm build` 成功，`templates/page_cooperation.html` 含 `data-endpoint` 注入

## 阶段 4：后端参考实现

- [ ] 新建 `worker/cooperation.ts`（Cloudflare Worker，含 CORS、校验、限流、Resend 发信、统一 JSON 响应）
- [ ] 新建 `worker/README.md`（环境变量、wrangler 部署、本地调试、前端填参说明，预留飞书 webhook 扩展注释）
- [ ] 可选：`worker/wrangler.toml.example` 示例配置
- [ ] 验证：`npx tsc --noEmit --project worker/tsconfig.json` 或至少 `pnpm check` 不报错（若 worker 独立则文档说明）

## 阶段 5：整体验收

- [ ] `pnpm check` 通过
- [ ] `pnpm build` 通过，产物 `templates/page_cooperation.html` 无 iframe/feishu
- [ ] 手动场景：
  - [ ] 空值/非法手机号触发行内错误
  - [ ] 蜜罐有值静默成功
  - [ ] 未配置 endpoint 显示提示且不发 fetch
  - [ ] 已配置 endpoint（可用 https://httpbin.org/post 临时测试）成功切成功态、失败保留数据

## 验证命令

```bash
pnpm check
pnpm build
grep -rn "feishu\|iframe" templates/ || echo "no feishu/iframe residual"
```

## 风险文件与回滚点

- 高风险：`settings.yaml`（表单结构）、`src/page_cooperation.html`（大段替换）、`src/js/main.ts`（引入新模块）
- 每阶段完成后单独 commit，回滚点为上一 commit

## 提交前检查

- [ ] `templates/` 为构建产物已在 .gitignore，无需手动改
- [ ] 无硬编码演示文案残留，成功态文案来自 `theme.config.cooperation.*`
- [ ] 全库无 `feishu.cn` 残留（除 worker 注释中可选扩展提及）
