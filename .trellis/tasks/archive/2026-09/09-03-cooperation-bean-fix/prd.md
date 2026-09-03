# 修复cooperation-plugin启动ClassCastException

## Goal
修复 `cooperation-plugin 1.1.0` 启动时 `Bean$Bootstrap` 在 `PluginClassLoader` 与宿主 `app` ClassLoader 重复加载导致的 `ClassCastException`，使插件可在 Halo 2.20.11 上正常启用并显示 Console 管理页。

## Background
- 报错堆栈：`ConfigurationClassBeanDefinitionReader.loadBeanDefinitionsForBeanMethod` 中 `Bean$Bootstrap` 强转失败，`PluginClassLoader@757f8903` 与 `app` 的同名类冲突
- 现状 `build.gradle` 将 `spring-boot-starter-mail` 以 `implementation` 打入 Jar，连带 `spring-context/spring-beans` 被插件独立加载，与宿主重复
- 1.0.0 亦有隐患，1.1.0 首次安装触发完整配置类解析后暴露
- Halo 宿主已提供 Spring Boot 3.2.5 及相关 starters，插件不应重复打包

## Requirements
- 将 `org.springframework.boot:spring-boot-starter-mail` 由 `implementation` 改为 `compileOnly`，与 `webflux/security/validation` 保持一致
- 确保插件 Jar 不再包含 `spring-beans-*.jar / spring-context-*.jar / spring-core-*.jar`（通过 `jar tf` 验证）
- 版本升至 `1.1.1`（`build.gradle` 与 `plugin.yaml` 同步），保持 `archiveBaseName = cooperation-plugin`
- 保持邮件发送能力：Halo 宿主已含 `JavaMailSender`，`EmailService` 无需额外依赖；若宿主缺失需在文档注明
- 重新构建并验证 `gradle :plugin-cooperation:build -x test` 成功，`console/main.js` 仍被正确打入

## Acceptance Criteria
- [ ] `build.gradle` 中所有 `spring-boot-starter-*` 均为 `compileOnly`
- [ ] `jar tf plugin-cooperation/build/libs/cooperation-plugin-1.1.1.jar | grep spring-beans` 无输出（未打包）
- [ ] 在 Halo 2.20.11 本地或 1Panel 宿主上安装 `1.1.1` 后，插件状态为 `STARTED`，日志 `合作咨询插件启动成功`，无 `ClassCastException`
- [ ] Console 菜单“合作咨询”仍可见，POST 提交与 GET 列表/导出/删除/标记已处理均正常
- [ ] 邮件配置为空时仍可提交（跳过发信），配置完整时可正常发信

## Notes
- 轻量修复任务，PRD-only，无需 design.md/implement.md
- 回滚：保留 1.1.0 Jar 作对比，1.1.1 若发信缺失则需改回 implementation 并用 `exclude` 排除 spring-beans
