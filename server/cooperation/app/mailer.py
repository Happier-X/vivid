"""邮件发送封装，支持 SSL 465 与 STARTTLS 587."""

from __future__ import annotations

import logging
import smtplib
import ssl
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText

logger = logging.getLogger(__name__)


def build_subject(company: str, type_label: str | None) -> str:
    label = type_label or ""
    if label:
        return f"【万椿官网】合作咨询 - {company} - {label}"
    return f"【万椿官网】合作咨询 - {company}"


def build_body(data: dict) -> str:
    lines = [
        f"公司名称：{data.get('company', '')}",
        f"联系人：{data.get('contact', '')}",
        f"联系电话：{data.get('phone', '')}",
        f"合作类型：{data.get('type', '')}（{data.get('typeLabel', '')}）",
        f"合作意向：{data.get('message', '') or '（未填写）'}",
        "",
        f"来源页面：{data.get('sourceUrl', '')}",
        f"浏览器：{data.get('userAgent', '')}",
        f"提交时间：{data.get('timestamp', '')}",
        f"客户端 IP：{data.get('ip', '')}",
    ]
    return "\n".join(lines)


def build_html_body(data: dict) -> str:
    rows = [
        ("公司名称", data.get("company", "")),
        ("联系人", data.get("contact", "")),
        ("联系电话", data.get("phone", "")),
        ("合作类型", f"{data.get('type', '')}（{data.get('typeLabel', '')}）"),
        ("合作意向", data.get("message", "") or "（未填写）"),
        ("来源页面", data.get("sourceUrl", "")),
        ("浏览器", data.get("userAgent", "")),
        ("提交时间", data.get("timestamp", "")),
        ("客户端 IP", data.get("ip", "")),
    ]
    trs = "".join(f"<tr><td style='padding:6px 12px;border:1px solid #ddd;font-weight:bold;background:#f7f7f7'>{k}</td><td style='padding:6px 12px;border:1px solid #ddd'>{v}</td></tr>" for k, v in rows)
    return f"<html><body><h3 style='color:#2b8d89'>万椿官网合作咨询</h3><table style='border-collapse:collapse;width:100%'>{trs}</table></body></html>"


def send_cooperation_email(
    *,
    smtp_host: str,
    smtp_port: int,
    smtp_user: str,
    smtp_pass: str,
    smtp_from: str,
    receiver_email: str,
    smtp_use_ssl: bool,
    data: dict,
) -> None:
    """同步发送邮件，失败抛异常由调用方处理."""
    if not smtp_host or not receiver_email:
        logger.warning("SMTP 未配置，跳过邮件发送")
        return

    subject = build_subject(data.get("company", ""), data.get("typeLabel"))
    body_text = build_body(data)
    body_html = build_html_body(data)

    receivers = [r.strip() for r in receiver_email.split(",") if r.strip()]
    if not receivers:
        logger.warning("接收邮箱为空，跳过邮件发送")
        return

    msg = MIMEMultipart("alternative")
    msg["Subject"] = subject
    msg["From"] = smtp_from or smtp_user
    msg["To"] = ", ".join(receivers)
    msg.attach(MIMEText(body_text, "plain", "utf-8"))
    msg.attach(MIMEText(body_html, "html", "utf-8"))

    logger.info("准备发送邮件至 %s 主题 %s", receivers, subject)

    if smtp_use_ssl or smtp_port == 465:
        context = ssl.create_default_context()
        with smtplib.SMTP_SSL(smtp_host, smtp_port, context=context, timeout=10) as server:
            if smtp_user and smtp_pass:
                server.login(smtp_user, smtp_pass)
            server.send_message(msg)
    else:
        with smtplib.SMTP(smtp_host, smtp_port, timeout=10) as server:
            # 587 STARTTLS
            try:
                server.starttls(context=ssl.create_default_context())
            except Exception:
                # 若不支持 STARTTLS 则直接发送
                pass
            if smtp_user and smtp_pass:
                server.login(smtp_user, smtp_pass)
            server.send_message(msg)

    logger.info("邮件发送成功")
