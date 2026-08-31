"""数据留存：SQLite + JSONL 追加写入."""

from __future__ import annotations

import json
import logging
import sqlite3
import threading
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

logger = logging.getLogger(__name__)

_lock = threading.Lock()
_initialized = False
_db_path: Path | None = None
_log_path: Path | None = None


def init_store(db_path: str | Path, log_path: str | Path) -> None:
    global _initialized, _db_path, _log_path
    _db_path = Path(db_path)
    _log_path = Path(log_path)
    # 确保目录存在
    _db_path.parent.mkdir(parents=True, exist_ok=True)
    _log_path.parent.mkdir(parents=True, exist_ok=True)
    with _lock:
        if _initialized:
            return
        conn = sqlite3.connect(str(_db_path))
        try:
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS cooperations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    company TEXT NOT NULL,
                    contact TEXT NOT NULL,
                    phone TEXT NOT NULL,
                    type TEXT NOT NULL,
                    type_label TEXT,
                    message TEXT,
                    source_url TEXT,
                    user_agent TEXT,
                    timestamp TEXT,
                    ip TEXT,
                    created_at TEXT NOT NULL
                )
                """
            )
            conn.commit()
        finally:
            conn.close()
        _initialized = True
        logger.info("存储初始化完成 db=%s log=%s", _db_path, _log_path)


def _ensure_initialized():
    if not _initialized or _db_path is None or _log_path is None:
        # 默认路径（相对当前工作目录，避免 import 时崩溃）
        init_store("cooperations.db", "cooperations.log")


def save_cooperation(data: dict) -> int:
    """保存一条记录，返回自增 id，同时追加 JSONL."""
    _ensure_initialized()
    assert _db_path is not None and _log_path is not None
    created_at = datetime.now(timezone.utc).isoformat()
    record = {
        "company": data.get("company", "") or "",
        "contact": data.get("contact", "") or "",
        "phone": data.get("phone", "") or "",
        "type": data.get("type", "") or "",
        "type_label": data.get("typeLabel", "") or "",
        "message": data.get("message", "") or "",
        "source_url": data.get("sourceUrl", "") or "",
        "user_agent": data.get("userAgent", "") or "",
        "timestamp": data.get("timestamp", "") or "",
        "ip": data.get("ip", "") or "",
        "created_at": created_at,
    }
    with _lock:
        conn = sqlite3.connect(str(_db_path))
        try:
            cur = conn.execute(
                """
                INSERT INTO cooperations (company, contact, phone, type, type_label, message, source_url, user_agent, timestamp, ip, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    record["company"],
                    record["contact"],
                    record["phone"],
                    record["type"],
                    record["type_label"],
                    record["message"],
                    record["source_url"],
                    record["user_agent"],
                    record["timestamp"],
                    record["ip"],
                    record["created_at"],
                ),
            )
            conn.commit()
            row_id = cur.lastrowid or 0
        finally:
            conn.close()
        # JSONL 追加
        try:
            with open(_log_path, "a", encoding="utf-8") as f:
                json.dump({**record, "id": row_id}, f, ensure_ascii=False)
                f.write("\n")
        except Exception as e:
            logger.warning("写入 JSONL 失败: %s", e)
    return row_id


def list_cooperations(page: int = 1, size: int = 20) -> tuple[list[dict[str, Any]], int]:
    """分页查询，返回 (rows, total)."""
    _ensure_initialized()
    assert _db_path is not None
    page = max(1, page)
    size = max(1, min(size, 100))
    offset = (page - 1) * size
    with _lock:
        conn = sqlite3.connect(str(_db_path))
        conn.row_factory = sqlite3.Row
        try:
            total = conn.execute("SELECT COUNT(*) FROM cooperations").fetchone()[0]
            cur = conn.execute(
                "SELECT * FROM cooperations ORDER BY id DESC LIMIT ? OFFSET ?", (size, offset)
            )
            rows = [dict(r) for r in cur.fetchall()]
        finally:
            conn.close()
    return rows, total
