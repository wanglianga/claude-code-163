package com.city.manhole.controller;

import com.city.manhole.common.*;
import com.city.manhole.config.AuthInterceptor;
import com.city.manhole.entity.User;
import com.city.manhole.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository userRepo;
    private final TokenService tokenService;

    public record LoginReq(@NotBlank String username, @NotBlank String password) {}

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody LoginReq req) {
        User user = userRepo.findByUsername(req.username())
                .orElseThrow(() -> new BizException("用户名或密码错误"));
        if (!user.getPassword().equals(Passwords.sha256(req.password()))) {
            throw new BizException("用户名或密码错误");
        }
        String token = tokenService.create(user.getId());
        user.setPassword(null);
        return ApiResponse.ok(Map.of("token", token, "user", user));
    }

    @GetMapping("/me")
    public ApiResponse<?> me() {
        User u = AuthContext.require();
        u.setPassword(null);
        return ApiResponse.ok(u);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        tokenService.remove(AuthInterceptor.resolveToken(request));
        return ApiResponse.ok();
    }
}
