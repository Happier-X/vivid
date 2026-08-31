# 实施计划：合作表单独立后端 FastAPI 服务

## 前置

- [ ] 确认本机 Python 3.11+、uv 已安装（`uv --version`），或 pip 可用
- [ ] 阅读 `src/js/cooperation-form.ts` 的提交契约与校验规则

## 阶段 1：工程脚手架

- [ ] 新建 `server/cooperation/` 目录，`pyproject.toml` + `app/__init__.py` + `.env.example`
- [ ] `Dockerfile`（`python:3.11-slim` + `uv sync`）与 `docker-compose.yml`（port 8000）
- [ ] 验证：`uv sync` 成功，`uv run python -c "import fastapi"` 无报错

## 阶段 2：核心模型与工具

- [ ] `app/schemas.py`：Pydantic 模型 + `PHONE_RE` + 校验器
- [ ] `app/rate_limiter.py`：内存限流实现
- [ ] `app/mailer.py`：SMTP 发送封装（同步 `smtplib` 优先）
- [ ] `app/store.py`：SQLite 初始化与追加（或 jsonl 日志）
- [ ] 验证：`uv run python -m py_compile app/*.py` 通过

## 阶段 3：FastAPI 路由

- [ ] `app/main.py`：`FastAPI` 实例、`CORSMiddleware`、`POST /api/cooperation`、`GET /health`、`GET /api/cooperations`（API_KEY 鉴权）
- [ ] 蜜罐分支、统一响应 `{success,message}`、422→400 转换、限流 429
- [ ] 验证：`uv run uvicorn app.main:app --port 8001 &`，`curl GET /health` 与 `POST /api/cooperation` 联调

## 阶段 4：主题联动

- [ ] `vivid/settings.yaml` 默认 `cooperation.endpoint` 改为 `/api/cooperation`
- [ ] `pnpm check` 与 `pnpm build` 通过，`templates/page_cooperation.html` 含新路径

## 阶段 5：文档与部署

- [ ] `server/cooperation/README.md`：本地 uv 启动、docker、systemd、Nginx 反代、主题配置、curl 示例、排错
- [ ] `.env.example` 含全部 SMTP 与限流配置说明
- [ ] 验证：`grep -rn feishu server/cooperation/` 无命中

## 阶段 6：整体验收

- [ ] `uv sync && uv run uvicorn app.main:app --host 0.0.0.0 --port 8000` 正常
- [ ] `docker compose config` 通过（如本机有 docker）
- [ ] `pnpm check/build` 通过
- [ ] 手动：POST 非法 payload → 400，蜜罐 → 200，限流 → 429，正常 → 200 并写库/日志

## 验证命令

```bash
uv sync
uv run uvicorn app.main:app --host 127.0.0.1 --port 8001 &
curl http://127.0.0.1:8001/health
curl -X POST http://127.0.0.1:8001/api/cooperation -H "Content-Type: application/json" -d '{"company":"测试","contact":"张三","phone":"13812345678","type":"institution","message":"","website":""}'
pnpm check
pnpm build
```

## 风险文件与回滚

- 高风险：`server/cooperation/app/main.py`（路由）、`server/cooperation/app/schemas.py`（校验）、`settings.yaml`
- 回滚：停用 Nginx 反代，主题 endpoint 清空

## 提交前检查

- [ ] `server/cooperation/` 下无硬编码密码，敏感配置来自 `.env`
- [ ] `server/cooperation/build/` 等产物未提交，`.gitignore` 已覆盖
- [ ] 全库无 feishu 残留
