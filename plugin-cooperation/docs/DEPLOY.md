# 部署手册（cooperation-plugin 1.1.0）

> 自 1.1.0 起，合作咨询已由 Halo 插件统一承载并提供 Console 后台管理；`server/cooperation`（FastAPI）与 `worker/cooperation.ts`（Cloudflare Worker）已 **废弃 Deprecated**，保留目录仅供历史参考，不再推荐部署。主题 `endpoint` 固定为插件同源路径。

## 前置

- Halo >=2.20.0，已部署于自有服务器，与主题同域
- JDK 17+、Gradle 8.x、Node 18+、pnpm 10+（1.1.0 新增 Console 构建）
- SMTP 邮箱（企业邮 / QQ 企业邮 / 163 等），获取 Host/Port/授权码

## 构建（1.1.0 新增 Console 步骤）

```bash
# 1. 构建 Console 前端（输出到 src/main/resources/console）
cd plugin-cooperation/console
pnpm install
pnpm build
ls ../src/main/resources/console/main.js   # 应存在

# 2. 构建插件（产物含 console 入口）
cd ../..  # 回到根
./gradlew :plugin-cooperation:build -x test
ls plugin-cooperation/build/libs/cooperation-plugin-1.1.0.jar
jar tf plugin-cooperation/build/libs/cooperation-plugin-1.1.0.jar | grep console
# console/main.js
# console/style.css
```

Halo 插件工具 `run.halo.plugin.devtools:0.6.2` 会在构建期校验 `plugin.yaml` 并生成 `plugin-cooperation/build`。

## 安装

```bash
# 假设 Halo 安装目录为 /opt/halo
cp plugin-cooperation/build/libs/cooperation-plugin-1.1.0.jar /opt/halo/plugins/
# 重启 Halo
systemctl restart halo
# 或 docker 重启
docker restart halo
```

控制台 → 插件 → 合作咨询插件 → 启用。启用后左侧菜单出现“合作咨询”。

> 若从 1.0.0 升级：直接覆盖 Jar 并重启即可，历史数据 `spec.handled` 自动兼容为 `false`，无需迁移。

## SMTP 配置

控制台 → 插件 → 合作咨询插件 → 设置：

| 字段             | 示例                      |
| ---------------- | ------------------------- |
| smtpHost         | smtp.qiye.163.com         |
| smtpPort         | 465                       |
| smtpUsername     | noreply@wanchunsmart.com  |
| smtpPassword     | 授权码                    |
| fromEmail        | noreply@wanchunsmart.com  |
| receiverEmail    | contact@wanchunsmart.com  |
| smtpSsl          | true（465）/ false（587） |
| rateLimitSeconds | 60                        |

保存后立即生效，无需重启。若留空 Host/Receiver，插件仅落库不发信并记录 WARN。

### 测试发信

提交一次合作表单，观察：

- 前端是否 `200 {success:true}`
- Halo 日志是否 `合作咨询邮件已发送至 ...`
- 收件箱是否收到标题 `【万椿官网】合作咨询 - {company} - {typeLabel}` 的邮件

若 `500`，检查：

- Host/Port 是否可达（465 需 SSL，587 需 STARTTLS）
- 用户名/密码是否正确（部分邮箱需授权码而非登录密码）
- 防火墙是否放行出站 SMTP

## 主题联动

`settings.yaml` 中 `cooperation.endpoint` 默认值已为：

```
/apis/api.cooperation.vivid.run/v1alpha1/cooperations
```

控制台 → 主题 → 万椿主题 → 设置 → 合作咨询表单 → 表单提交接口 确认为同源路径。

前端同源 `fetch POST JSON`，Nginx 无需额外 CORS 配置：

```nginx
# Halo 与主题同域同端口，无跨域
location /apis/api.cooperation.vivid.run/ {
  proxy_pass http://halo:8090;
}
```

`pnpm build` 会将 `src/js/cooperation-form.ts` 的 `isValidEndpoint` 同时支持 `https://` 与 `/apis/...`，避免历史上仅 `https://` 的校验误拦截同源路径。

## Console 后台使用

- 入口：Halo Console 左侧“合作咨询”（需 `ROLE_ADMIN`，未登录 401 跳登录，非管理员 403）
- 顶部：关键词（company/contact/phone 模糊）+ 合作类型单选 + 处理状态单选（全部/已处理/未处理）+ 时间区间（creationTimestamp）+ 搜索/重置 + 导出 CSV（当前筛选结果，无数据时置灰）
- 表格列：公司名称、联系人、联系电话、合作类型（含中文标签）、合作意向（截断）、提交时间、IP、来源页面、状态标签
- 分页：默认 `size=20`，可切换 10/20/50
- 详情抽屉：展示全部字段 `company/contact/phone/type/typeLabel/message/sourceUrl/userAgent/timestamp/ip/creationTimestamp/handled`，支持切换已处理状态
- 删除：二次确认，调用 `DELETE /.../cooperations/{name}`，成功后自动刷新
- 导出：`GET /.../cooperations/export`，UTF-8 BOM，Excel 中文不乱码
- 空状态：友好提示引导检查表单是否已发布

## 查看数据

### 通过 Halo Console（推荐）

左侧“合作咨询”即为可视化列表，支持筛选/分页/详情/删除/导出。

### 通过 Halo API（需管理员）

```bash
# 列表（支持筛选）
curl "http://localhost:8090/apis/api.cooperation.vivid.run/v1alpha1/cooperations?page=0&size=20&keyword=测试&type=institution&handled=false" \
  -H "Cookie: halo-SESSION=xxx" | jq

# 详情
curl "http://localhost:8090/apis/api.cooperation.vivid.run/v1alpha1/cooperations/{name}" -H "Cookie: ..."

# 标记已处理
curl -X PUT "http://localhost:8090/apis/api.cooperation.vivid.run/v1alpha1/cooperations/{name}/handled" \
  -H "Content-Type: application/json" -H "Cookie: ..." -d '{"handled":true}'

# 删除
curl -X DELETE "http://localhost:8090/apis/api.cooperation.vivid.run/v1alpha1/cooperations/{name}" -H "Cookie: ..."

# 导出 CSV
curl "http://localhost:8090/apis/api.cooperation.vivid.run/v1alpha1/cooperations/export?keyword=测试" -H "Cookie: ..." -o cooperations.csv
```

### 通过数据库（H2/Postgres）

Halo 将 Extension 存于 `extensions` 表，可通过 Halo 控制台 → 系统 → 扩展管理 查看 `api.cooperation.vivid.run` Group 下的 `Cooperation`。

## 限流与风控

- 默认 IP 维度 60s 1 次，内存 `ConcurrentHashMap<String,Long>` + 同步清理
- 多实例部署时退化为单机限流，如需分布式请替换 `RateLimiter` 为 Redis 实现
- 蜜罐字段 `website` 仅前端隐藏输入，机器人填值即静默成功，不落库不发信

## 回滚

- 停用插件：控制台 → 插件 → 停用
- 覆盖回退：保留旧 Jar `cooperation-plugin-1.0.0.jar`，出问题直接替换重启；历史数据仍可读（多出的 handled 字段被忽略）
- 主题端将 `cooperation.endpoint` 清空或改回外部 Worker 地址，前端即回退为 mailto 提示（不推荐）

## 常见问题

| 现象              | 原因                      | 处理                                                                  |
| ----------------- | ------------------------- | --------------------------------------------------------------------- |
| POST 404          | 插件未启用或 Halo 未重启  | 检查 `plugins/` 下 Jar 与控制台启用状态                               |
| GET 401           | 未登录或非管理员          | 使用管理员账号登录后携带 Cookie/Token，Console 会自动跳登录           |
| GET 403           | 已登录但非管理员          | Console 提示无权限，需 ROLE_ADMIN                                     |
| 菜单不显示        | console 未打包或 Jar 旧版 | 检查 `jar tf ... \| grep console`，重新 `pnpm build` + `gradle build` |
| POST 429          | 同一 IP 60s 内重复提交    | 等待或下调 `rateLimitSeconds`                                         |
| POST 500 邮件失败 | SMTP 配置错误             | 检查日志 `EmailService`，更正 Host/Port/密码                          |
| 导出乱码          | 未带 BOM 或 Excel 编码    | 后端已带 UTF-8 BOM，请用 Excel/WPS 直接打开                           |

## 废弃声明

- `server/cooperation`（FastAPI）与 `worker/cooperation.ts` 自 1.1.0 起废弃，文档顶部已标注 Deprecated，不再维护 `API_KEY`/SQLite 方案；目录保留但不再作为推荐部署路径。
- 主题 `settings.yaml` 的 `endpoint` 已固定为插件路径，不再提供 FastAPI 的 `/api/cooperation` 作为默认值。
