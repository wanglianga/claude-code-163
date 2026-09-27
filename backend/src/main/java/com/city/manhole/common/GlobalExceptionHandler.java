package com.city.manhole.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> biz(BizException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> other(Exception e) {
        return ApiResponse.error("服务器错误: " + e.getMessage());
    }
}
