"""IP 维度内存限流，惰性清理."""

from __future__ import annotations

import threading
import time

_store: dict[str, float] = {}
_lock = threading.Lock()


def try_acquire(ip: str, seconds: int) -> bool:
    """尝试获取令牌，成功返回 True，被限流返回 False."""
    now = time.time()
    with _lock:
        # 惰性清理过期条目
        expired = [k for k, ts in _store.items() if now - ts > seconds]
        for k in expired:
            del _store[k]
        last = _store.get(ip)
        if last is not None and now - last < seconds:
            return False
        _store[ip] = now
        return True


def clear() -> None:
    """测试辅助：清空状态."""
    with _lock:
        _store.clear()


def get_remaining(ip: str, seconds: int) -> int:
    """返回剩余限流秒数（0 表示不限流）."""
    now = time.time()
    with _lock:
        last = _store.get(ip)
        if last is None:
            return 0
        elapsed = now - last
        if elapsed >= seconds:
            return 0
        return int(seconds - elapsed)
