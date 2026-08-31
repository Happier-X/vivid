# 部署手册（cooperation-plugin）

## 前置

- Halo >=2.20.0，已部署于自有服务器，与主题同域
- JDK 17+，Gradle 8.x 可用
- SMTP 邮箱（企业邮 / QQ 企业邮 / 163 等），获取 Host/Port/授权码

## 构建

```bash
./gradlew :plugin-cooperation:build -x test
ls plugin-cooperation/build/libs/cooperation-plugin-1.0.0.jar
```

Halo 插件工具 `run.halo.plugin.devtools:0.6.2` 会在构建期校验 `plugin.yaml` 并生成 `plugin-cooperation/build`。

## 安装

```bash
# 假设 Halo 安装目录为 /opt/halo
cp plugin-cooperation/build/libs/cooperation-plugin-1.0.0.jar /opt/halo/plugins/
# 重启 Halo
systemctl restart halo
# 或 docker 重启
docker restart halo
```

控制台 → 插件 → 合作咨询插件 → 启用。

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

## 查看数据

### 通过 Halo API（需管理员）

```bash
curl "http://localhost:8090/apis/api.cooperation.vivid.run/v1alpha1/cooperations?page=0&size=20" \
  -H "Cookie: halo-admin-token=xxx" | jq
```

或详情：

```bash
curl "http://localhost:8090/apis/api.cooperation.vivid.run/v1alpha1/cooperations/{name}" \
  -H "Cookie: ..."
```

### 通过数据库（H2/Postgres）

Halo 将 Extension 存于 `extensions` 思考（实际表为 `extensions`），可通过 Halo 控制台 → 系统 → 扩展管理 查看 `api.cooperation.vivid.run` Group 下的 `Cooperation`。

## 限流与风控

- 默认 IP 维度 60s 1 次，内存 `ConcurrentHashMap<String,Long>` + 同步清理
- 多实例部署时退化为单机限流，如需分布式请替换 `RateLimiter` 为 Redis 实现
- 蜜罐字段 `website` 仅前端隐藏输入，机器人填值即静默成功，不落库不发信

## 回滚

- 停用插件：控制台 → 插件 → 停用
- 主题端将 `cooperation.endpoint` 清空或改回外部 Worker 地址，前端即回退为 mailto 提示

## 常见问题

| 现象              | 原因                     | 处理                                         |
| ----------------- | ------------------------ | -------------------------------------------- |
| POST 404          | 插件未启用或 Halo 未重启 | 检查 `plugins/` 下 Jar 与控制台启用状态      |
| GET 401           | 未登录或非管理员         | 使用管理员账号登录后携带 Cookie/Token        |
| POST 429          | 同一 IP 60s 内重复提交   | 等待或下调 `rateLimitSeconds`                |
| POST 500 邮件失败 | SMTP 配置错误            | 检查日志 `EmailService`，更正 Host/Port/密码 |
