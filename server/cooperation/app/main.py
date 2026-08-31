"""FastAPI 入口：健康检查、合作提交、分页查询."""

from __future__ import annotations

import logging
from contextlib import asynccontextmanager
from pathlib import Path
from typing import Any

from fastapi import FastAPI, Header, Query, Request
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic_settings import BaseSettings, SettingsConfigDict

from . import rate_limiter, store
from .mailer import send_cooperation_email
from .schemas import CooperationRequest

logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s %(message)s")


BASE_DIR = Path(__file__).resolve().parent.parent
ENV_PATH = BASE_DIR / ".env"


class Settings(BaseSettings):
    port: int = 8000
    allowed_origins: str = ""
    rate_limit_seconds: int = 60
    smtp_host: str = ""
    smtp_port: int = 465
    smtp_user: str = ""
    smtp_pass: str = ""
    smtp_from: str = "noreply@wanchunsmart.com"
    receiver_email: str = "contact@wanchunsmart.com"
    smtp_use_ssl: bool = True
    api_key: str = ""
    db_path: str = "cooperations.db"
    log_path: str = "cooperations.log"

    model_config = SettingsConfigDict(env_file=str(ENV_PATH), env_file_encoding="utf-8", extra="ignore")


settings = Settings()

DB_PATH = BASE_DIR / settings.db_path if not Path(settings.db_path).is_absolute() else Path(settings.db_path)
LOG_PATH = BASE_DIR / settings.log_path if not Path(settings.log_path).is_absolute() else Path(settings.log_path)


@asynccontextmanager
async def lifespan(app: FastAPI):
    store.init_store(str(DB_PATH), str(LOG_PATH))
    yield


app = FastAPI(title="Cooperation API", version="1.0.0", lifespan=lifespan)

# CORS：同源反代无需 CORS，但支持白名单
if settings.allowed_origins:
    origins = [o.strip() for o in settings.allowed_origins.split(",") if o.strip()]
    # 支持 "*" 表示全部
    if "*" in origins:
        app.add_middleware(
            CORSMiddleware,
            allow_origins=["*"],
            allow_credentials=True,
            allow_methods=["*"],
            allow_headers=["*"],
        )
    elif origins:
        app.add_middleware(
            CORSMiddleware,
            allow_origins=origins,
            allow_credentials=True,
            allow_methods=["*"],
            allow_headers=["*"],
        )


def _client_ip(request: Request) -> str:
    # 优先 X-Forwarded-For（Nginx 反代）
    xff = request.headers.get("x-forwarded-for")
    if xff:
        return xff.split(",")[0].strip()
    x_real = request.headers.get("x-real-ip")
    if x_real:
        return x_real.strip()
    if request.client:
        return request.client.host
    return "unknown"


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    # 将 422 转为 400 统一响应
    errors = exc.errors()
    # 取第一条错误信息
    first = errors[0] if errors else {}
    msg = first.get("msg", "参数错误")
    # 中文友好：去掉前缀 "Value error, "
    if ", " in msg:
        # Pydantic 会前缀类型
        parts = msg.split(", ", 1)
        if parts[0].lower().startswith("value error"):
            msg = parts[1]
    # 针对必填字段定制
    if "Field required" in str(first.get("type", "")):
        field = ".".join(str(x) for x in first.get("loc", [])[1:]) if len(first.get("loc", [])) > 1 else "参数"
        msg = f"{field} 为必填项"
        if field == "company":
            msg = "请输入公司名称"
        elif field == "contact":
            msg = "请输入联系人"
        elif field == "phone":
            msg = "请输入联系电话"
        elif field == "type":
            msg = "请选择合作类型"
    # 长度错误中文映射
    if "String should have at least" in msg or "at_least" in str(first.get("type", "")):
        field = first.get("loc", ["", ""])[-1] if first.get("loc") else ""
        if field == "company":
            msg = "公司名称至少 2 个字符"
        elif field == "contact":
            msg = "联系人至少 2 个字符"
        elif "at most" in msg or "too_long" in str(first.get("type", "")):
            field2 = first.get("loc", ["", ""])[-1] if first.get("loc") else ""
            if field2 == "company":
                msg = "公司名称不能超过 50 个字符"
            elif field2 == "contact":
                msg = "联系人不能超过 20 个字符"
            elif field2 == "message":
                msg = "合作意向说明不能超过 500 个字符"
    if "too_long" in str(first.get("type", "")):
        field = first.get("loc", ["", ""])[-1] if first.get("loc") else ""
        if field == "company":
            msg = "公司名称不能超过 50 个字符"
        elif field == "contact":
            msg = "联系人不能超过 20 个字符"
        elif field == "message":
            msg = "合作意向说明不能超过 500 个字符"
    return JSONResponse(status_code=400, content={"success": False, "message": msg})


@app.get("/health")
async def health():
    return {"status": "ok"}


@app.post("/api/cooperation")
async def post_cooperation(payload: CooperationRequest, request: Request):
    ip = _client_ip(request)

    # 蜜罐：有值直接静默成功，不落库不发邮件不计限流
    if payload.website and payload.website.strip():
        logger.info("蜜罐命中 ip=%s", ip)
        return {"success": True, "message": "提交成功"}

    # 限流
    if not rate_limiter.try_acquire(ip, settings.rate_limit_seconds):
        remaining = rate_limiter.get_remaining(ip, settings.rate_limit_seconds)
        return JSONResponse(
            status_code=429,
            content={"success": False, "message": f"提交过于频繁，请 {remaining} 秒后再试"},
        )

    data: dict[str, Any] = payload.model_dump()
    data["ip"] = ip

    # 落库
    try:
        store.save_cooperation(data)
    except Exception as e:
        logger.exception("落库失败: %s", e)
        return JSONResponse(status_code=500, content={"success": False, "message": "服务异常，请稍后重试"})

    # 发邮件（若未配置则仅日志）
    try:
        if settings.smtp_host and settings.receiver_email:
            send_cooperation_email(
                smtp_host=settings.smtp_host,
                smtp_port=settings.smtp_port,
                smtp_user=settings.smtp_user,
                smtp_pass=settings.smtp_pass,
                smtp_from=settings.smtp_from,
                receiver_email=settings.receiver_email,
                smtp_use_ssl=settings.smtp_use_ssl,
                data=data,
            )
        else:
            logger.info("SMTP 未配置，跳过邮件发送 ip=%s company=%s", ip, data.get("company"))
    except Exception as e:
        logger.exception("邮件发送失败: %s", e)
        return JSONResponse(status_code=500, content={"success": False, "message": "邮件发送失败，请稍后重试"})

    return {"success": True, "message": "提交成功"}


@app.get("/api/cooperations")
async def list_cooperations(
    request: Request,
    page: int = Query(default=1, ge=1),
    size: int = Query(default=20, ge=1, le=100),
    x_api_key: str | None = Header(default=None, alias="X-API-KEY"),
    authorization: str | None = Header(default=None),
):
    # 鉴权：X-API-KEY 或 Authorization: Bearer <key>
    api_key = x_api_key
    if not api_key and authorization:
        if authorization.lower().startswith("bearer "):
            api_key = authorization[7:].strip()
        else:
            api_key = authorization.strip()
    expected = settings.api_key
    if not expected:
        return JSONResponse(status_code=500, content={"success": False, "message": "服务端未配置 API_KEY"})
    if not api_key or api_key != expected:
        return JSONResponse(status_code=401, content={"success": False, "message": "未授权"})

    rows, total = store.list_cooperations(page=page, size=size)
    return {"success": True, "message": "ok", "data": {"items": rows, "total": total, "page": page, "size": size}}
