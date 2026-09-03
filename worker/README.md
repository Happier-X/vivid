> ⚠️ **已废弃 Deprecated**：自 `cooperation-plugin 1.1.0` 起，合作咨询已由 Halo 插件同源接口统一承载，主题 `endpoint` 固定为 `/apis/api.cooperation.vivid.run/v1alpha1/cooperations`，本 Worker 仅保留作历史参考，不再推荐部署。

# 合作咨询表单 Worker 部署说明

参考实现：`worker/cooperation.ts`（Cloudflare Workers / Workerd）

## 功能

- 校验 `company/contact/phone/type/message` 与蜜罐 `website`
- 内存级限流：同一 IP 默认 60 秒 1 次（`RATE_LIMIT_SECONDS` 可配）
- 通过 Resend 发送邮件（`RESEND_API_KEY`），未配置时仅日志透出
- CORS 预检与白名单
- 返回统一 JSON `{ success: boolean, message?: string }`

## 环境变量

| 变量                 | 必填   | 说明                                                                                                  |
| -------------------- | ------ | ----------------------------------------------------------------------------------------------------- |
| `RECEIVER_EMAIL`     | 是     | 接收合作咨询的邮箱                                                                                    |
| `RESEND_API_KEY`     | 二选一 | Resend API Key（推荐）                                                                                |
| `RESEND_FROM`        | 否     | 发件人，默认 `万椿官网 <noreply@wanchunsmart.com>`                                                    |
| `ALLOWED_ORIGINS`    | 否     | 逗号分隔的白名单 Origin，留空则允许所有；例如 `https://www.wanchunsmart.com,https://wanchunsmart.com` |
| `RATE_LIMIT_SECONDS` | 否     | 限流窗口秒数，默认 `60`                                                                               |
| `SMTP_*`             | 否     | 如需 SMTP 请改用自建 Node 服务，Worker 示例默认 Resend                                                |

> 二选一说明：本示例优先 Resend（`fetch https://api.resend.com/emails`）。如需 SMTP，因 Worker 无原生 SMTP，可部署为 Node 服务并使用 `nodemailer`，或在 Worker 中通过第三方邮件 API 中转。

## 本地调试

```bash
# 安装 wrangler（若未安装）
npm i -g wrangler

# 本地开发（需配置 wrangler.toml 或 --var）
wrangler dev worker/cooperation.ts --var RECEIVER_EMAIL:contact@wanchunsmart.com --var RESEND_API_KEY:re_xxx --var ALLOWED_ORIGINS:*

# 或使用配置文件
cp worker/wrangler.toml.example wrangler.toml
# 编辑 wrangler.toml 填入对应 vars
wrangler dev
```

测试提交：

```bash
curl -X POST http://127.0.0.1:8787/api/cooperation \
  -H "Content-Type: application/json" \
  -H "Origin: https://example.com" \
  -d '{"company":"测试公司","contact":"张三","phone":"13812345678","type":"institution","typeLabel":"养老机构合作","message":"想了解合作","website":"","sourceUrl":"https://example.com/cooperation","userAgent":"curl","timestamp":"2026-08-31T00:00:00.000Z"}'
```

预期：`{"success":true,"message":"提交成功"}`

## 部署

```bash
# 1. 登录
wrangler login

# 2. 部署
wrangler deploy worker/cooperation.ts --name vivid-cooperation --var RECEIVER_EMAIL:contact@wanchunsmart.com --var RESEND_API_KEY:re_xxx

# 或通过 wrangler.toml 部署
wrangler deploy
```

部署后获得地址例如 `https://vivid-cooperation.xxx.workers.dev/api/cooperation`，将其填入 Halo 后台：主题设置 → 合作咨询表单 → 表单提交接口。

前端通过 `theme.config.cooperation.endpoint` 以 `data-endpoint` 注入，修改后即时生效，无需重新构建。

## 前端联调

- `endpoint` 留空：表单显示“表单提交功能未配置，请通过右侧电话/邮箱联系”，并提供 `mailto:receiver_email`
- 已配置：`fetch POST application/json`，10 秒超时，成功切成功态、失败保留数据

## CORS 常见问题

- 若前端报 CORS 错误，检查 `ALLOWED_ORIGINS` 是否包含当前站点 Origin，或临时设为 `*`（生产建议白名单）
- 预检 `OPTIONS` 已处理 `Access-Control-Allow-Origin/Methods/Headers`
