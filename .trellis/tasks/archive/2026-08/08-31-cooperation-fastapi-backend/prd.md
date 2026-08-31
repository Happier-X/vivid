# 合作表单独立后端 FastAPI 服务

## 目标

用 Python FastAPI 编写独立于 Halo 插件的合作表单后端，提供同源 `POST /api/cooperation` 接口，实现校验、蜜罐、限流、邮件通知与可选落库，通过 Nginx 同源反代与主题联动，无需重启 Halo。

## 背景与约束

- 前置：`08-31-cooperation-form-native` 已改为原生表单 `fetch POST JSON` 到 `theme.config.cooperation.endpoint`，`plugin-cooperation` 为 Halo 插件方案但需 Jar 部署+重启
- 用户新决策：改用 Python FastAPI 独立服务，部署于自有服务器（与 Halo 同机或同内网），通过 Nginx `location /api/cooperation` 反代到 FastAPI（默认 8000 端口）
- 运行：`uv + FastAPI + Uvicorn`，Python 3.11+
- 主题保持纯静态，仅修改 `settings.yaml` 默认 endpoint 为同源相对路径

## 需求

### R1 服务工程

- 目录 `server/cooperation/`：`pyproject.toml`（uv 管理）、`app/main.py`、`app/schemas.py`、`app/rate_limiter.py`、`app/mailer.py`、`Dockerfile`、`docker-compose.yml`、`.env.example`
- 依赖：`fastapi uvicorn[standard] pydantic[email] python-multipart email-validator`，可选 `aiosmtplib` 或 `smtplib` 同步
- 一键启动：`uv sync && uv run uvicorn app.main:app --host 0.0.0.0 --port 8000`，`docker compose up -d` 亦支持

### R2 提交接口（匿名）

- `POST /api/cooperation`：`Content-Type: application/json`
- 请求体与前端一致：
  ```json
  {
    "company": "",
    "contact": "",
    "phone": "",
    "type": "institution|community|home_government|channel_oem",
    "typeLabel": "",
    "message": "",
    "website": "",
    "sourceUrl": "",
    "userAgent": "",
    "timestamp": ""
  }
  ```
- 响应统一 `{ "success": boolean, "message": string }`，`200/400/429/500`，蜜罐有值 → `200` 静默成功

### R3 校验

- `company: 必填 2-50`，`contact: 必填 2-20`，`phone: 必填 正则 ^(1[3-9]\d{9}|0\d{2,3}-?\d{7,8})$`，`type: 4枚举`，`message: 0-500`，`website: 必须为空`
- 使用 Pydantic `field_validator` + 正则，与前端 `PHONE_RE` 一致，错误信息与前端语义对齐

### R4 限流与风控

- IP 维度内存限流默认 `60s 1次`，可通过 `.env` 的 `RATE_LIMIT_SECONDS` 配置，`dict[str,float]` + 惰性清理
- 蜜罐 `website` 非空直接返回成功不落库不发邮件

### R5 邮件通知

- 通过 `smtplib`/`aiosmtplib` 发送，配置来自 `.env`：`SMTP_HOST/PORT/USER/PASS/FROM/RECEIVER`（支持 SSL 465 与 STARTTLS 587）
- 标题 `【万椿官网】合作咨询 - {company} - {typeLabel}`，正文含全字段 + `sourceUrl/userAgent/timestamp/ip`
- 发送失败记录日志并返回 `500`

### R6 数据留存

- 首期：SQLite `cooperations.db`（`sqlite3` + `SQLAlchemy` 可选，首期可用 `aiosqlite` 或直接写 `jsonl` 日志），提供 `GET /api/cooperations`（需鉴权，`X-API-KEY` 或 `Authorization`）分页查询
- 如无需查询，至少提供 `cooperations.log` JSONL 追加写入，便于 `tail` 审计

### R7 运维与部署

- `GET /health` 健康检查返回 `{status:"ok"}`
- CORS：同源反代无需 CORS，但支持 `.env` 的 `ALLOWED_ORIGINS` 白名单（开发时 `*`）
- `Dockerfile` 基于 `python:3.11-slim`，`docker-compose.yml` 暴露 8000，`Nginx` 示例 `location /api/cooperation { proxy_pass http://127.0.0.1:8000; }`
- 提供 `server/cooperation/README.md`：本地 `uv` 启动、`docker` 启动、`systemd`/`pm2`（可选）、Nginx 配置、主题 endpoint 配置、curl 联调

### R8 主题联动

- `settings.yaml` 中 `cooperation.endpoint` 默认值改为 `/api/cooperation`（FastAPI 同源路径）
- 前端 `cooperation-form.ts` 的 `isValidEndpoint` 已支持 `/api/...` 相对路径，无需再改

## 非目标

- Halo 插件 Jar（本任务为独立服务，插件保留但不作为主路径）
- 管理后台 UI（首期仅 API + 日志/SQLite）
- 分布式限流（Redis）

## 验收标准

- [ ] 1. `uv sync && uv run uvicorn app.main:app` 可启动，`GET /health` 返回 `200 {"status":"ok"}`
- [ ] 2. `POST /api/cooperation` 匿名可访问，非法 payload 返回 `400`，蜜罐返回 `200`，同 IP 60s 内二次返回 `429`
- [ ] 3. 配置 SMTP 后提交成功收到邮件，`cooperations.db` 或日志中有记录
- [ ] 4. `docker compose up` 可一键启动，`curl` 联调通过
- [ ] 5. 主题 `settings.yaml` 默认 endpoint 为 `/api/cooperation`，`pnpm check/build` 通过，`templates/page_cooperation.html` 含新路径
- [ ] 6. `server/cooperation/` 下 `grep -ri feishu` 无命中，提供 `README.md` 与 `.env.example`
- [ ] 7. `pnpm check` 通过

## 约束

- 遵循 `halo-theme.md` 主题契约，endpoint 双向同步
- 不引入飞书相关代码
- Python 依赖锁定（`uv.lock` 或 `requirements.txt`）
