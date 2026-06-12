package com.faceai.pdfreader.ai.skills;

import com.faceai.pdfreader.ai.client.AiClient;
import com.faceai.pdfreader.ai.prompts.PromptTemplates;
import com.faceai.pdfreader.model.ImageDescriptionResponse;
import com.faceai.pdfreader.service.PdfService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ImageDescriptionSkill {

    private final AiClient aiClient;
    private final PdfService pdfService;

    public ImageDescriptionSkill(AiClient aiClient, PdfService pdfService) {
        this.aiClient = aiClient;
        this.pdfService = pdfService;
    }

    public ImageDescriptionResponse execute(String fileId, String imageUrl) {
        Path imagePath = pdfService.resolveStoredFileFromUrl(imageUrl);
        if (!Files.exists(imagePath)) {
            throw new IllegalArgumentException("图片文件不存在，无法进行图片说明");
        }

        String imageBase64;
        try {
            imageBase64 = Base64.getEncoder().encodeToString(Files.readAllBytes(imagePath));
        } catch (IOException ex) {
            throw new IllegalArgumentException("读取图片失败，无法进行图片说明");
        }

        List<Map<String, Object>> content = List.of(
                Map.of("type", "text", "text", PromptTemplates.imageDescriptionPrompt()),
                Map.of(
                        "type", "image_url",
                        "image_url", Map.of("url", "data:%s;base64,%s".formatted(detectMimeType(imagePath), imageBase64))
                )
        );

        String description = aiClient.multimodal(content);
        return new ImageDescriptionResponse(fileId, imageUrl, description);
    }

    private String detectMimeType(Path imagePath) {
        try {
            String mimeType = Files.probeContentType(imagePath);
            if (StringUtils.hasText(mimeType)) {
                return mimeType;
            }
        } catch (IOException ignored) {
        }
        String fileName = imagePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".png")) {
            return "image/png";
        }
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }
}
