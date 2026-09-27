package com.city.manhole.controller;

import com.city.manhole.common.ApiResponse;
import com.city.manhole.common.AuthContext;
import com.city.manhole.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @PostMapping("/upload")
    public ApiResponse<?> upload(@RequestParam("file") MultipartFile file) throws Exception {
        AuthContext.require();
        if (file.isEmpty()) throw new BizException("文件为空");
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
        String name = UUID.randomUUID().toString().replace("-", "") + ext;
        Path dir = Path.of(uploadDir).toAbsolutePath();
        Files.createDirectories(dir);
        Files.copy(file.getInputStream(), dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        return ApiResponse.ok(Map.of("url", "/uploads/" + name, "name", original));
    }
}
