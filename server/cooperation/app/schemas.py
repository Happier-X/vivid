"""Pydantic 模型与校验规则，与前端 cooperation-form.ts 保持一致."""

from __future__ import annotations

import re
from typing import Literal

from pydantic import BaseModel, Field, field_validator

PHONE_RE = re.compile(r"^(1[3-9]\d{9}|0\d{2,3}-?\d{7,8})$")
ALLOWED_TYPES = {"institution", "community", "home_government", "channel_oem"}

CooperationType = Literal["institution", "community", "home_government", "channel_oem"]


class CooperationRequest(BaseModel):
    company: str = Field(..., min_length=2, max_length=50, description="公司名称")
    contact: str = Field(..., min_length=2, max_length=20, description="联系人")
    phone: str = Field(..., description="手机号或座机")
    type: str = Field(..., description="合作类型枚举")
    typeLabel: str | None = Field(default=None, description="合作类型展示名")
    message: str = Field(default="", max_length=500, description="合作意向说明")
    website: str = Field(default="", description="蜜罐字段，必须为空")
    sourceUrl: str = Field(default="", description="来源 URL")
    userAgent: str = Field(default="", description="浏览器 UA")
    timestamp: str = Field(default="", description="前端时间戳")

    @field_validator("company", mode="before")
    @classmethod
    def _strip_company(cls, v):
        if isinstance(v, str):
            return v.strip()
        return v

    @field_validator("contact", mode="before")
    @classmethod
    def _strip_contact(cls, v):
        if isinstance(v, str):
            return v.strip()
        return v

    @field_validator("phone", mode="before")
    @classmethod
    def _strip_phone(cls, v):
        if isinstance(v, str):
            return v.strip()
        return v

    @field_validator("message", mode="before")
    @classmethod
    def _strip_message(cls, v):
        if isinstance(v, str):
            return v.strip()
        return v

    @field_validator("phone")
    @classmethod
    def validate_phone(cls, v: str) -> str:
        if not v:
            raise ValueError("请输入联系电话")
        if not PHONE_RE.match(v):
            raise ValueError("请输入正确的手机号或座机号")
        return v

    @field_validator("type")
    @classmethod
    def validate_type(cls, v: str) -> str:
        if not v or not v.strip():
            raise ValueError("请选择合作类型")
        if v not in ALLOWED_TYPES:
            raise ValueError("合作类型不合法")
        return v

    @field_validator("website")
    @classmethod
    def validate_website(cls, v: str) -> str:
        # 蜜罐校验不在此处抛错，由路由层静默处理；此处仅保留字段
        return v or ""


class CooperationResponse(BaseModel):
    success: bool
    message: str


class HealthResponse(BaseModel):
    status: str = "ok"
