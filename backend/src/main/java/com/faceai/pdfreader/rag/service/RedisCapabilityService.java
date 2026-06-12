package com.faceai.pdfreader.rag.service;

import com.faceai.pdfreader.model.RedisCapabilityResponse;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisCapabilityService {

    private final StringRedisTemplate redisTemplate;

    public RedisCapabilityService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public RedisCapabilityResponse inspect() {
        try {
            String pong = redisTemplate.execute(RedisConnection::ping);
            boolean reachable = "PONG".equalsIgnoreCase(pong);
            if (!reachable) {
                return new RedisCapabilityResponse(false, false, "Redis 未返回 PONG");
            }
            boolean vectorReady = supportsVectorSearch();
            return new RedisCapabilityResponse(
                    true,
                    vectorReady,
                    vectorReady
                            ? "Redis 可连接，且具备向量检索相关能力"
                            : "Redis 可连接，但未检测到 Redis Stack / RediSearch 向量检索能力"
            );
        } catch (DataAccessException ex) {
            String message = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
            return new RedisCapabilityResponse(false, false, "Redis 不可连接: " + message);
        } catch (Exception ex) {
            return new RedisCapabilityResponse(false, false, "Redis 检测失败: " + ex.getMessage());
        }
    }

    private boolean supportsVectorSearch() {
        Object result = redisTemplate.execute((RedisConnection connection) -> connection.execute("FT._LIST"));
        return result != null;
    }
}
