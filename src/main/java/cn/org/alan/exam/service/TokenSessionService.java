package cn.org.alan.exam.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * 按 JWT 管理登录会话，使同一浏览器中的多个标签页可以分别登录不同账号。
 */
@Service
public class TokenSessionService {

    private static final String TOKEN_PREFIX = "token:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Value("${jwt.expiration}")
    private long expiration;

    public void store(String token) {
        String normalizedToken = normalize(token);
        if (normalizedToken == null) {
            return;
        }
        stringRedisTemplate.opsForValue().set(
                buildKey(normalizedToken), "1", expiration, TimeUnit.MILLISECONDS);
    }

    public boolean isActive(String token) {
        String normalizedToken = normalize(token);
        return normalizedToken != null
                && Boolean.TRUE.equals(stringRedisTemplate.hasKey(buildKey(normalizedToken)));
    }

    public void replace(String oldToken, String newToken) {
        revoke(oldToken);
        store(newToken);
    }

    public void revoke(String token) {
        String normalizedToken = normalize(token);
        if (normalizedToken != null) {
            stringRedisTemplate.delete(buildKey(normalizedToken));
        }
    }

    public String normalize(String token) {
        if (token == null) {
            return null;
        }
        String normalizedToken = token.trim();
        if (normalizedToken.startsWith("Bearer ")) {
            normalizedToken = normalizedToken.substring(7).trim();
        }
        return normalizedToken.isEmpty() ? null : normalizedToken;
    }

    private String buildKey(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                hex.append(String.format("%02x", value & 0xff));
            }
            return TOKEN_PREFIX + hex;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256", e);
        }
    }
}
