package com.faceai.pdfreader.service;

import com.faceai.pdfreader.config.StorageProperties;
import com.faceai.pdfreader.model.ImageContentResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageService {

    private final StorageProperties storageProperties;

    public ImageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public ImageContentResponse upload(MultipartFile file) {
        validateImage(file);
        String fileId = UUID.randomUUID().toString().replace("-", "");
        String safeFileName = StringUtils.cleanPath(file.getOriginalFilename());
        Path uploadPath = resolveRoot().resolve(Paths.get(storageProperties.imageUploadDir(), fileId, safeFileName));

        try {
            Files.createDirectories(uploadPath.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, uploadPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return new ImageContentResponse(
                    fileId,
                    safeFileName,
                    "/files/" + storageProperties.imageUploadDir() + "/" + fileId + "/" + safeFileName
            );
        } catch (IOException ex) {
            throw new IllegalArgumentException("保存图片失败");
        }
    }

    public ImageContentResponse getContent(String fileId) {
        Path imagePath = resolveImagePath(fileId);
        return new ImageContentResponse(
                fileId,
                imagePath.getFileName().toString(),
                "/files/" + storageProperties.imageUploadDir() + "/" + fileId + "/" + imagePath.getFileName()
        );
    }

    public Path resolveImagePath(String fileId) {
        Path fileDir = resolveRoot().resolve(storageProperties.imageUploadDir()).resolve(fileId);
        if (!Files.exists(fileDir) || !Files.isDirectory(fileDir)) {
            throw new IllegalArgumentException("图片文件不存在");
        }
        try (var files = Files.list(fileDir)) {
            return files.findFirst().orElseThrow(() -> new IllegalArgumentException("未找到图片文件"));
        } catch (IOException ex) {
            throw new IllegalArgumentException("读取图片失败");
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传图片文件");
        }
        String fileName = file.getOriginalFilename();
        if (!StringUtils.hasText(fileName)) {
            throw new IllegalArgumentException("图片文件名不能为空");
        }
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        if (!(lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".bmp") || lowerName.endsWith(".webp"))) {
            throw new IllegalArgumentException("仅支持 png/jpg/jpeg/bmp/webp 图片");
        }
        long maxBytes = storageProperties.maxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("文件大小不能超过 " + storageProperties.maxFileSizeMb() + "MB");
        }
    }

    private Path resolveRoot() {
        return Paths.get(storageProperties.rootDir()).toAbsolutePath().normalize();
    }
}
