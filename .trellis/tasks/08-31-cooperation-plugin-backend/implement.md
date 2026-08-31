# 实施计划：自建合作表单 Halo 插件后端

## 前置

- [ ] 阅读 `.trellis/spec/frontend/halo-theme.md` 与 Halo 插件官方模板（`https://github.com/halo-dev/plugin-starter`）
- [ ] 确认本机 JDK 17+、Gradle 8.x 可用，Halo 2.20+ 运行环境

## 阶段 1：工程脚手架

- [ ] 新建 `plugin-cooperation/` 目录，`build.gradle` + `settings.gradle` + `gradle-wrapper`（或复用 `plugin-starter` 模板）
- [ ] `src/main/resources/plugin.yaml`（元数据 + settingName）
- [ ] 插件设置 `src/main/resources/extensions/setting.yaml` 或直接 `plugin.yaml` 关联的 `cooperation-setting`
- [ ] `CooperationPlugin.java` 继承 `BasePlugin`，空实现启动钩子
- [ ] 验证：`./gradlew :plugin-cooperation:build -x test` 生成 Jar，`plugin.yaml` 语法校验

## 阶段 2：Extension 与 DTO

- [ ] `extension/Cooperation.java` 定义 `@GVK` 与 `Spec`
- [ ] `dto/CooperationRequest.java` 加 Jakarta Validation 注解（`@NotBlank @Size @Pattern`），常量 `PHONE_RE`
- [ ] `CooperationProperties.java` 映射插件设置（`@ConfigurationProperties`）
- [ ] 验证：`./gradlew check` 无编译错误

## 阶段 3：核心路由与服务

- [ ] `RateLimiter.java` 内存限流实现
- [ ] `EmailService.java` 基于 `JavaMailSenderImpl` 动态配置
- [ ] `CooperationService.java` 编排校验→限流→落库（`ReactiveExtensionClient`）→发信
- [ ] `CooperationController.java` 提供 `RouterFunction`：
  - `POST /apis/api.cooperation.vivid.run/v1alpha1/cooperations` permitAll
  - `GET /apis/api.cooperation.vivid.run/v1alpha1/cooperations` authenticated
- [ ] 蜜罐分支与统一 `CooperationResponse` `{success,message}`
- [ ] 验证：`./gradlew test`（如有）或至少 `build` 通过

## 阶段 4：主题联动

- [ ] `vivid/settings.yaml` 中 `cooperation.endpoint` 默认值改为 `/apis/api.cooperation.vivid.run/v1alpha1/cooperations`
- [ ] `src/page_cooperation.html` 无需改动（已通过 `data-endpoint` 注入）
- [ ] `pnpm check` 与 `pnpm build` 通过，`templates/page_cooperation.html` 含新默认路径

## 阶段 5：文档与部署

- [ ] `plugin-cooperation/README.md`：功能、构建、安装（拷贝 Jar 到 Halo plugins）、重启、插件设置 SMTP、主题 endpoint 配置、联调 `curl` 示例
- [ ] `plugin-cooperation/docs/DEPLOY.md` 可选：Nginx 同源说明、无 CORS 原因、限流与邮件排错、查看 Extension 数据的 `kubectl`/`curl` 示例
- [ ] 验证：`grep -rn "feishu" plugin-cooperation/` 无命中

## 阶段 6：整体验收

- [ ] `./gradlew :plugin-cooperation:build` 成功
- [ ] `pnpm check` / `pnpm build` 成功
- [ ] 手动场景（需 Halo 实例）：
  - [ ] 匿名 POST 正常提交并可在 `GET /apis/.../cooperations` 查到
  - [ ] 非法 phone/空 company 返回 400
  - [ ] 蜜罐有值返回 200 且不发信不落库
  - [ ] 60s 内二次提交返回 429

## 验证命令

```bash
./gradlew :plugin-cooperation:build -x test
pnpm check
pnpm build
grep -rn "feishu" plugin-cooperation/ || echo "no feishu"
```

## 风险文件与回滚

- 高风险：`plugin-cooperation/build.gradle`（插件依赖）、`src/main/java/.../CooperationController.java`（路由）、`vivid/settings.yaml`（默认值）
- 回滚：停用插件并将主题 `endpoint` 清空或切回 Worker 即可

## 提交前检查

- [ ] 插件 Jar 未提交到 git（仅源码），`.gitignore` 包含 `plugin-cooperation/build/`
- [ ] 无硬编码 SMTP 密码，配置来自插件设置
- [ ] 全库无 feishu 残留
