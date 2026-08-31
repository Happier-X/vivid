# 技术设计：合作表单独立后端 FastAPI 服务

## 架构总览

```
Browser (page_cooperation.html /api/cooperation 相对路径)
  ↓ Nginx (同源反代)
  ↓ proxy_pass 127.0.0.1:8000
FastAPI (server/cooperation/app/main.py)
  ├─ 校验(Pydantic) → 蜜罐 → 限流(内存) → 落库(SQLite/jsonl) → 发邮件(smtplib)
  └─ /health, /api/cooperation (POST), /api/cooperations (GET, 需 API_KEY)
Halo Console (主题设置 cooperation.endpoint = /api/cooperation)
```

同源无 CORS，独立于 Halo 进程，Nginx 统一入口。

## 工程结构

```
server/cooperation/
├── pyproject.toml            // uv, fastapi, uvicorn, pydantic, aiosqlite
├── .env.example              // SMTP_* / ALLOWED_ORIGINS / RATE_LIMIT_SECONDS / API_KEY
├── Dockerfile                // python:3.11-slim, uv sync
├── docker-compose.yml        // port 8000, volume .env + db
├── app/
│   ├── __init__.py
│   ├── main.py               // FastAPI app, lifespan, router
│   ├── schemas.py            // CooperationRequest/Response, PHONE_RE
│   ├── rate_limiter.py       // dict + lock
│   ├── mailer.py             // send_cooperation_email()
│   └── store.py              // SQLite/jsonl 追加
└── README.md
```

### pyproject.toml 关键

```toml
[project]
name = "cooperation-server"
requires-python = ">=3.11"
dependencies = ["fastapi>=0.110", "uvicorn[standard]>=0.30", "pydantic>=2.7", "pydantic-settings>=2.3", "aiosqlite>=0.20", "aiosmtplib>=3.0"]

[tool.uv]
```

或用 `requirements.txt` 锁定。

### 配置 (.env / Settings)

```python
class Settings(BaseSettings):
    smtp_host: str = ""
    smtp_port: int = 465
    smtp_user: str = ""
    smtp_pass: str = ""
    smtp_from: str = "noreply@wanchunsmart.com"
    receiver_email: str = "contact@wanchunsmart.com"
    allowed_origins: str = ""  # 逗号分隔，空则同源不限
    rate_limit_seconds: int = 60
    api_key: str = ""  # GET 查询需 X-API-KEY
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")
```

### schemas.py

```python
PHONE_RE = re.compile(r"^(1[3-9]\d{9}|0\d{2,3}-?\d{7,8})$")
ALLOWED_TYPES = {"institution","community","home_government","channel_oem"}

class CooperationRequest(BaseModel):
    company: str = Field(min_length=2, max_length=50)
    contact: str = Field(min_length=2, max_length=20)
    phone: str
    type: str
    typeLabel: str | None = None
    message: str = Field(default="", max_length=500)
    website: str = ""
    sourceUrl: str = ""
    userAgent: str = ""
    timestamp: str = ""

    @field_validator("phone")
    def validate_phone(cls, v): ...
    @field_validator("type")
    def validate_type(cls, v): ...
```

### rate_limiter.py

```python
_store: dict[str, float] = {}
_lock = threading.Lock()

def try_acquire(ip: str, seconds: int) -> bool:
    now = time.time()
    with _lock:
        # 惰性清理过期
        for k, ts in list(_store.items()):
            if now - ts > seconds: del _store[k]
        last = _store.get(ip)
        if last and now - last < seconds: return False
        _store[ip] = now
        return True
```

### mailer.py

- 使用 `smtplib` 同步或 `aiosmtplib` 异步，`SMTP_SSL` 465 vs `SMTP+STARTTLS` 587 自动切换
- `MIMEText` + `MIMEMultipart`，标题含 `company/typeLabel`

### main.py

- `FastAPI(title="Cooperation API")`
- `CORSMiddleware` 根据 `ALLOWED_ORIGINS` 配置
- `POST /api/cooperation`：`request: CooperationRequest`, `request.client.host` 取 IP，`await` 限流→校验（Pydantic 自动 422，需转为 400 统一）→蜜罐→store→mailer
- 统一错误处理：将 Pydantic `422` 转为 `{success:false, message:"..."} 400`
- `GET /api/cooperations?page&size`：校验 `X-API-KEY == settings.api_key`，分页读 SQLite
- `GET /health`：`{"status":"ok"}`

## 主题联动

- `vivid/settings.yaml` 默认 `cooperation.endpoint = /api/cooperation`
- 前端已支持相对路径，无需改动

## 部署

- 本地：`uv sync && uv run uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload`
- Docker：`docker compose up -d --build`
- Systemd：`uv run uvicorn ...` 实参
- Nginx：`location /api/cooperation { proxy_pass http://127.0.0.1:8000; proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; }`

## 兼容与回滚

- 未配置 SMTP 时仍落库，仅日志告警
- 回滚：Nginx 移除反代，主题 endpoint 清空即回退

## 风险

- SMTP 配置错误 → `500`，文档提供 `curl` 测试步骤
- SQLite 并发写需加锁，首期可用 `aiosqlite` 串行化
