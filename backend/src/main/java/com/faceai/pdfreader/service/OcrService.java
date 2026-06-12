package com.faceai.pdfreader.service;

import com.faceai.pdfreader.config.OcrProperties;
import com.faceai.pdfreader.config.StorageProperties;
import com.faceai.pdfreader.model.OcrPageResult;
import com.faceai.pdfreader.model.OcrResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class OcrService {

    private final StorageProperties storageProperties;
    private final OcrProperties ocrProperties;
    private final ImageService imageService;

    public OcrService(StorageProperties storageProperties, OcrProperties ocrProperties, ImageService imageService) {
        this.storageProperties = storageProperties;
        this.ocrProperties = ocrProperties;
        this.imageService = imageService;
    }

    public OcrResponse extractTextFromPdf(String fileId) {
        ensureOcrReady();
        Path fileDir = resolveRoot().resolve(storageProperties.uploadDir()).resolve(fileId);
        if (!Files.exists(fileDir) || !Files.isDirectory(fileDir)) {
            throw new IllegalArgumentException("文件不存在");
        }

        try (var files = Files.list(fileDir)) {
            Path pdfPath = files
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".pdf"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("未找到 PDF 文件"));
            return runOcr(fileId, pdfPath);
        } catch (IOException ex) {
            throw new IllegalArgumentException("OCR 读取 PDF 失败");
        }
    }

    public OcrResponse extractTextFromImage(String fileId) {
        ensureOcrReady();
        Path imagePath = imageService.resolveImagePath(fileId);
        try {
            BufferedImage image = ImageIO.read(imagePath.toFile());
            if (image == null) {
                throw new IllegalArgumentException("无法解析图片内容");
            }
            String text = doOcr(createTesseract(), image);
            return new OcrResponse(
                    fileId,
                    imagePath.getFileName().toString(),
                    1,
                    text,
                    List.of(new OcrPageResult(
                            1,
                            text,
                            "/files/" + storageProperties.imageUploadDir() + "/" + fileId + "/" + imagePath.getFileName()
                    ))
            );
        } catch (IOException ex) {
            throw new IllegalArgumentException("读取图片失败，无法执行 OCR");
        }
    }

    private OcrResponse runOcr(String fileId, Path pdfPath) throws IOException {
        Path pageImageDir = resolveRoot().resolve(ocrProperties.pageImageDir()).resolve(fileId);
        Files.createDirectories(pageImageDir);
        List<OcrPageResult> pageResults = new ArrayList<>();
        StringBuilder fullText = new StringBuilder();

        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            PDFRenderer renderer = new PDFRenderer(document);
            ITesseract tesseract = createTesseract();
            int pageCount = document.getNumberOfPages();

            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                BufferedImage pageImage = renderer.renderImageWithDPI(
                        pageIndex,
                        ocrProperties.renderDpi(),
                        ImageType.RGB
                );
                String imageName = "page-" + (pageIndex + 1) + "." + normalizeImageFormat();
                Path imagePath = pageImageDir.resolve(imageName);
                ImageIO.write(pageImage, normalizeImageFormat(), imagePath.toFile());

                String pageText = doOcr(tesseract, pageImage);
                pageResults.add(new OcrPageResult(
                        pageIndex + 1,
                        pageText,
                        "/files/" + ocrProperties.pageImageDir() + "/" + fileId + "/" + imageName
                ));

                if (!pageText.isBlank()) {
                    if (fullText.length() > 0) {
                        fullText.append(System.lineSeparator()).append(System.lineSeparator());
                    }
                    fullText.append("第 ").append(pageIndex + 1).append(" 页").append(System.lineSeparator())
                            .append(pageText);
                }
            }

            return new OcrResponse(
                    fileId,
                    pdfPath.getFileName().toString(),
                    pageCount,
                    fullText.toString(),
                    pageResults
            );
        }
    }

    private ITesseract createTesseract() {
        Tesseract tesseract = new Tesseract();
        if (StringUtils.hasText(ocrProperties.dataPath())) {
            Path dataPath = Paths.get(ocrProperties.dataPath()).toAbsolutePath().normalize();
            if (!Files.exists(dataPath)) {
                throw new IllegalArgumentException("OCR dataPath 不存在，请检查 app.ocr.data-path 配置");
            }
            tesseract.setDatapath(dataPath.toString());
        }
        tesseract.setLanguage(StringUtils.hasText(ocrProperties.language()) ? ocrProperties.language() : "eng");
        return tesseract;
    }

    private String doOcr(ITesseract tesseract, BufferedImage pageImage) {
        try {
            return tesseract.doOCR(pageImage).trim();
        } catch (TesseractException ex) {
            throw new IllegalArgumentException(
                    "OCR 识别失败，请确认已安装 Tesseract 语言包，并正确配置 app.ocr.data-path 和 app.ocr.language"
            );
        }
    }

    private void ensureOcrReady() {
        if (!ocrProperties.enabled()) {
            throw new IllegalArgumentException("OCR 功能未开启，请在 application.yml 中设置 app.ocr.enabled=true");
        }
    }

    private String normalizeImageFormat() {
        return StringUtils.hasText(ocrProperties.imageFormat()) ? ocrProperties.imageFormat() : "png";
    }

    private Path resolveRoot() {
        return Paths.get(storageProperties.rootDir()).toAbsolutePath().normalize();
    }
}
