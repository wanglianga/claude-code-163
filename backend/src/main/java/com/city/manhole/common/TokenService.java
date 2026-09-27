package com.city.manhole.common;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 简易令牌服务：内存保存 token -> userId */
@Component
public class TokenService {
    private final Map<String, Long> tokens = new ConcurrentHashMap<>();

    public String create(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, userId);
        return token;
    }

    public Long getUserId(String token) {
        return token == null ? null : tokens.get(token);
    }

    public void remove(String token) {
        if (token != null) tokens.remove(token);
    }
}
