package com.faceai.pdfreader.service;

import com.faceai.pdfreader.config.StorageProperties;
import com.faceai.pdfreader.model.ExtractedImage;
import com.faceai.pdfreader.model.PageText;
import com.faceai.pdfreader.model.PdfContentResponse;
import com.faceai.pdfreader.model.UploadResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PdfService {

    private final StorageProperties storageProperties;

    public PdfService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public UploadResponse upload(MultipartFile file) {
        validateFile(file);
        String fileId = UUID.randomUUID().toString().replace("-", "");
        String safeFileName = StringUtils.cleanPath(file.getOriginalFilename());
        Path uploadPath = getUploadPath(fileId, safeFileName);

        try {
            Files.createDirectories(uploadPath.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, uploadPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return new UploadResponse(fileId, safeFileName);
        } catch (IOException ex) {
            throw new IllegalArgumentException("保存 PDF 文件失败");
        }
    }

    public PdfContentResponse getContent(String fileId) {
        Path fileDir = resolveRoot().resolve(storageProperties.uploadDir()).resolve(fileId);
        if (!Files.exists(fileDir) || !Files.isDirectory(fileDir)) {
            throw new IllegalArgumentException("文件不存在");
        }

        try (var files = Files.list(fileDir)) {
            Path pdfPath = files
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".pdf"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("未找到 PDF 文件"));

            return extractPdfContent(fileId, pdfPath);
        } catch (IOException ex) {
            throw new IllegalArgumentException("读取 PDF 失败");
        }
    }

    private PdfContentResponse extractPdfContent(String fileId, Path pdfPath) throws IOException {
        List<PageText> pageTexts = new ArrayList<>();
        List<ExtractedImage> images = new ArrayList<>();
        StringBuilder fullText = new StringBuilder();
        Path imageOutputDir = resolveRoot().resolve(storageProperties.imageDir()).resolve(fileId);
        Files.createDirectories(imageOutputDir);

        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            int totalPages = document.getNumberOfPages();

            for (int pageNumber = 1; pageNumber <= totalPages; pageNumber++) {
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);
                String pageText = stripper.getText(document).trim();
                pageTexts.add(new PageText(pageNumber, pageText));
                if (!pageText.isBlank()) {
                    if (fullText.length() > 0) {
                        fullText.append(System.lineSeparator()).append(System.lineSeparator());
                    }
                    fullText.append("第 ").append(pageNumber).append(" 页").append(System.lineSeparator())
                            .append(pageText);
                }
            }

            int imageIndex = 1;
            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                PDPage page = document.getPage(pageIndex);
                PDResources resources = page.getResources();
                for (COSName key : resources.getXObjectNames()) {
                    PDXObject xObject = resources.getXObject(key);
                    if (xObject instanceof PDImageXObject imageObject) {
                        BufferedImage bufferedImage = imageObject.getImage();
                        String extension = imageObject.getSuffix();
                        if (extension == null || extension.isBlank()) {
                            extension = "png";
                        }
                        String imageName = "page-" + (pageIndex + 1) + "-image-" + imageIndex + "." + extension;
                        Path imagePath = imageOutputDir.resolve(imageName);
                        ImageIO.write(bufferedImage, extension, imagePath.toFile());
                        images.add(new ExtractedImage(
                                pageIndex + 1,
                                imageName,
                                "/files/" + storageProperties.imageDir() + "/" + fileId + "/" + imageName
                        ));
                        imageIndex++;
                    }
                }
            }

            return new PdfContentResponse(
                    fileId,
                    pdfPath.getFileName().toString(),
                    totalPages,
                    "/files/" + storageProperties.uploadDir() + "/" + fileId + "/" + pdfPath.getFileName(),
                    fullText.toString(),
                    pageTexts,
                    images
            );
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传 PDF 文件");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("仅支持 PDF 文件");
        }
        long maxBytes = storageProperties.maxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("文件大小不能超过 " + storageProperties.maxFileSizeMb() + "MB");
        }
    }

    private Path getUploadPath(String fileId, String fileName) {
        return resolveRoot().resolve(Paths.get(storageProperties.uploadDir(), fileId, fileName));
    }

    public Path resolveStoredFileFromUrl(String fileUrl) {
        if (!StringUtils.hasText(fileUrl) || !fileUrl.startsWith("/files/")) {
            throw new IllegalArgumentException("图片地址格式不正确");
        }
        String relativePath = fileUrl.substring("/files/".length()).replace("/", java.io.File.separator);
        return resolveRoot().resolve(relativePath).normalize();
    }

    private Path resolveRoot() {
        return Paths.get(storageProperties.rootDir()).toAbsolutePath().normalize();
    }
}
