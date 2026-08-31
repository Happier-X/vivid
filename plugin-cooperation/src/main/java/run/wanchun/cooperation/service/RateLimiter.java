package run.wanchun.cooperation.service;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 内存限流器，IP 维度。
 * 使用 ConcurrentHashMap + 同步清理过期键。
 * 多实例下退化为单机限流，如需分布式请改用 Redis。
 */
@Component
public class RateLimiter {

    private final ConcurrentHashMap<String, Long> store = new ConcurrentHashMap<>();

    /**
     * 尝试获取令牌。
     * @param ip 客户端 IP
     * @param seconds 限流窗口秒数
     * @return true 允许通过，false 命中限流
     */
    public boolean tryAcquire(String ip, int seconds) {
        long now = System.currentTimeMillis();
        long windowMs = seconds * 1000L;

        // 先清理过期（每 100 次或遇到过期键时）
        cleanupExpired(now, windowMs);

        Long last = store.get(ip);
        if (last != null && now - last < windowMs) {
            return false;
        }
        store.put(ip, now);
        return true;
    }

    private synchronized void cleanupExpired(long now, long windowMs) {
        // 每次尝试获取时若存在过期键则清理，避免小容量 map 长期堆积
        if (store.isEmpty()) return;
        // 大容量下全量遍历，小容量下采样清理，兼顾性能与内存
        if (store.size() > 500) {
            Iterator<Map.Entry<String, Long>> it = store.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Long> entry = it.next();
                if (now - entry.getValue() > windowMs) {
                    it.remove();
                }
            }
        } else {
            store.entrySet().removeIf(e -> now - e.getValue() > windowMs);
        }
    }

    /**
     * 获取重试等待秒数。
     */
    public long getRetryAfterSeconds(String ip, int seconds) {
        Long last = store.get(ip);
        if (last == null) return 0;
        long elapsed = System.currentTimeMillis() - last;
        long remain = seconds * 1000L - elapsed;
        return remain > 0 ? (remain + 999) / 1000 : 0;
    }
}
