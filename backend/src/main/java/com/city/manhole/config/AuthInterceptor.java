package com.city.manhole.config;

import com.city.manhole.common.AuthContext;
import com.city.manhole.common.TokenService;
import com.city.manhole.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
    private final TokenService tokenService;
    private final UserRepository userRepository;

    public static String resolveToken(HttpServletRequest request) {
        String token = request.getHeader("X-Token");
        if (token == null || token.isBlank()) {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) token = auth.substring(7);
        }
        return token;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long userId = tokenService.getUserId(resolveToken(request));
        if (userId != null) {
            userRepository.findById(userId).ifPresent(AuthContext::set);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }
}
