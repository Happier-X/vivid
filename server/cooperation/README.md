> ⚠️ **已废弃 Deprecated**：自 `cooperation-plugin 1.1.0` 起，合作咨询已由 Halo 插件同源接口 `/apis/api.cooperation.vivid.run/v1alpha1/cooperations` 统一承载，并提供 Console 后台管理（列表/筛选/详情/删除/导出）。本 FastAPI 目录仅保留作历史参考，不再推荐部署，主题 `settings.yaml` 的 `endpoint` 已固定为插件路径。

# 合作表单独立后端（FastAPI）

独立于 Halo 插件的合作表单后端，提供同源 `POST /api/cooperation` 接口，实现校验、蜜罐、限流、邮件通知与 SQLite/JSONL 落库，通过 Nginx 同源反代与主题联动，无需重启 Halo。

## 架构

```
浏览器 (page_cooperation.html → /api/cooperation 相对路径)
  ↓ Nginx 同源反代
  ↓ proxy_pass 127.0.0.1:8000
FastAPI (server/cooperation/app/main.py)
  ├─ 校验(Pydantic) → 蜜罐 → 限流(内存) → 落库(SQLite/JSONL) → 发邮件(smtplib)
  └─ /health, /api/cooperation (POST), /api/cooperations (GET, 需 API_KEY)
```

## 目录

```
server/cooperation/
├── pyproject.toml
├── app/
│   ├── main.py
│   ├── schemas.py
│   ├── rate_limiter.py
│   ├── mailer.py
│   └── store.py
├── Dockerfile
├── docker-compose.yml
├── .env.example
└── README.md
```

## 快速开始

### 1. 本地 uv 启动

```bash
cd server/cooperation
cp .env.example .env   # 按需修改 SMTP / API_KEY
uv sync
uv run uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

验证：

```bash
curl http://127.0.0.1:8000/health
# {"status":"ok"}

curl -X POST http://127.0.0.1:8000/api/cooperation \
  -H "Content-Type: application/json" \
  -d '{"company":"测试公司","contact":"张三","phone":"13812345678","type":"institution","typeLabel":"养老机构合作","message":"合作意向","website":"","sourceUrl":"http://localhost","userAgent":"curl","timestamp":"2025-08-31T00:00:00Z"}'
# {"success":true,"message":"提交成功"}
```

### 2. Docker 启动

```bash
cd server/cooperation
docker compose up -d --build
docker compose logs -f
curl http://127.0.0.1:8000/health
```

### 3. systemd（可选）

```ini
# /etc/systemd/system/cooperation.service
[Unit]
Description=Cooperation FastAPI
After=network.target

[Service]
WorkingDirectory=/opt/vivid/server/cooperation
ExecStart=/root/.local/bin/uv run uvicorn app.main:app --host 127.0.0.1 --port 8000
Restart=always
EnvironmentFile=/opt/vivid/server/cooperation/.env

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now cooperation
```

## 环境变量 (.env)

| 变量               | 默认                     | 说明                                                  |
| ------------------ | ------------------------ | ----------------------------------------------------- |
| PORT               | 8000                     | 服务端口                                              |
| ALLOWED_ORIGINS    | 空                       | CORS 白名单，逗号分隔；同源反代建议留空；开发可设 `*` |
| RATE_LIMIT_SECONDS | 60                       | 同 IP 限流间隔（秒）                                  |
| SMTP_HOST          | 空                       | SMTP 服务器                                           |
| SMTP_PORT          | 465                      | 端口，465 SSL / 587 STARTTLS                          |
| SMTP_USER          | 空                       | 登录用户名                                            |
| SMTP_PASS          | 空                       | 登录密码                                              |
| SMTP_FROM          | noreply@wanchunsmart.com | 发件人                                                |
| RECEIVER_EMAIL     | contact@wanchunsmart.com | 收件人，多个逗号分隔                                  |
| SMTP_USE_SSL       | true                     | 是否强制 SSL（465）                                   |
| API_KEY            | 空                       | 查询接口鉴权密钥                                      |
| DB_PATH            | cooperations.db          | SQLite 路径                                           |
| LOG_PATH           | cooperations.log         | JSONL 日志路径                                        |

> 未配置 SMTP 时仍落库，仅日志提示，不影响提交成功。

## Nginx 反代示例

```nginx
location /api/cooperation {
    proxy_pass http://127.0.0.1:8000;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}

# 如需开放查询与健康检查
location /api/cooperations {
    proxy_pass http://127.0.0.1:8000;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
location /health {
    proxy_pass http://127.0.0.1:8000;
}
```

## 主题联动

`settings.yaml` 中 `cooperation.endpoint` 默认值已为 `/api/cooperation`（FastAPI 同源路径），前端 `cooperation-form.ts` 的 `isValidEndpoint` 已支持 `/api/...` 相对路径，无需额外修改。

Halo Console → 主题设置 → 合作咨询表单 → 表单提交接口，保持 `/api/cooperation` 即可。

## 接口说明

### GET /health

```json
{ "status": "ok" }
```

### POST /api/cooperation

- 匿名，无需鉴权
- `Content-Type: application/json`
- 请求体：

```json
{
  "company": "公司名称 2-50",
  "contact": "联系人 2-20",
  "phone": "13812345678 或 0531-66670365",
  "type": "institution|community|home_government|channel_oem",
  "typeLabel": "展示名",
  "message": "0-500",
  "website": "蜜罐必须为空",
  "sourceUrl": "",
  "userAgent": "",
  "timestamp": ""
}
```

- 响应统一 `{success:boolean, message:string}`，状态码：
  - `200` 成功（含蜜罐静默成功）
  - `400` 校验失败
  - `429` 限流（同 IP 60s 内二次）
  - `500` 邮件或落库异常

### GET /api/cooperations?page=1&size=20

需鉴权：`X-API-KEY: <API_KEY>` 或 `Authorization: Bearer <API_KEY>`

```bash
curl http://127.0.0.1:8000/api/cooperations?page=1&size=20 -H "X-API-KEY: change-me-please"
```

响应：

```json
{
  "success": true,
  "message": "ok",
  "data": {"items": [...], "total": 1, "page": 1, "size": 20}
}
```

## 数据审计

```bash
# SQLite 查询
sqlite3 cooperations.db "SELECT * FROM cooperations ORDER BY id DESC LIMIT 5;"
# JSONL 审计
tail -n 20 cooperations.log | jq .
```

## 联调 curl 示例

```bash
# 非法 payload → 400
curl -X POST http://127.0.0.1:8000/api/cooperation -H "Content-Type: application/json" -d '{"company":"A","contact":"","phone":"123","type":"bad","website":""}'

# 蜜罐 → 200 静默成功
curl -X POST http://127.0.0.1:8000/api/cooperation -H "Content-Type: application/json" -d '{"company":"测试","contact":"张三","phone":"13812345678","type":"institution","website":"spam"}'

# 限流 → 同 IP 60s 内二次 429
curl -X POST http://127.0.0.1:8000/api/cooperation -H "Content-Type: application/json" -d '{"company":"测试","contact":"张三","phone":"13812345678","type":"institution","website":""}'
curl -X POST http://127.0.0.1:8000/api/cooperation -H "Content-Type: application/json" -d '{"company":"测试2","contact":"李四","phone":"13900001111","type":"community","website":""}'

# 正常 → 200
curl -X POST http://127.0.0.1:8000/api/cooperation -H "Content-Type: application/json" -d '{"company":"万椿测试","contact":"王五","phone":"0531-66670365","type":"channel_oem","message":"OEM合作","website":""}'
```

## 排错

- `SMTP 未配置`：检查 `.env` 中 `SMTP_HOST/RECEIVER_EMAIL` 是否填写
- `邮件发送失败 500`：检查 `SMTP_PORT` 与 `SMTP_USE_SSL` 是否匹配（465 SSL / 587 STARTTLS），查看服务端日志
- `429 频繁`：等待 `RATE_LIMIT_SECONDS` 或临时调小该值
- `401 未授权`：`GET /api/cooperations` 需正确 `X-API-KEY`
