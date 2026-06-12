package com.faceai.pdfreader.service;

import com.faceai.pdfreader.auth.AuthContext;
import com.faceai.pdfreader.config.StorageProperties;
import com.faceai.pdfreader.model.DocumentUploadResponse;
import com.faceai.pdfreader.model.DocumentRecord;
import com.faceai.pdfreader.model.ImageContentResponse;
import com.faceai.pdfreader.model.PdfContentResponse;
import com.faceai.pdfreader.model.UploadResponse;
import com.faceai.pdfreader.rag.service.KnowledgeRagIndexService;
import com.faceai.pdfreader.repository.DocumentRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

    private static final Pattern DOCX_TEXT_NODE_PATTERN = Pattern.compile(
            "<(?:w|a):t(?:\\s[^>]*)?>([^<]*)</(?:w|a):t>"
    );

    private final StorageProperties storageProperties;
    private final PdfService pdfService;
    private final ImageService imageService;
    private final DocumentRepository documentRepository;
    private final ProfileService profileService;
    private final KnowledgeRagIndexService knowledgeRagIndexService;

    public DocumentService(
            StorageProperties storageProperties,
            PdfService pdfService,
            ImageService imageService,
            DocumentRepository documentRepository,
            ProfileService profileService,
            KnowledgeRagIndexService knowledgeRagIndexService
    ) {
        this.storageProperties = storageProperties;
        this.pdfService = pdfService;
        this.imageService = imageService;
        this.documentRepository = documentRepository;
        this.profileService = profileService;
        this.knowledgeRagIndexService = knowledgeRagIndexService;
    }

    public DocumentUploadResponse upload(MultipartFile file) {
        validateFile(file);
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".pdf")) {
            UploadResponse uploadResponse = pdfService.upload(file);
            PdfContentResponse content = pdfService.getContent(uploadResponse.fileId());
            DocumentUploadResponse response = new DocumentUploadResponse(
                    content.fileId(),
                    content.fileName(),
                    "PDF",
                    content.fileUrl(),
                    content.fullText()
            );
            saveRecord(response);
            return response;
        }
        if (isImage(lowerName)) {
            ImageContentResponse image = imageService.upload(file);
            DocumentUploadResponse response = new DocumentUploadResponse(
                    image.fileId(),
                    image.fileName(),
                    "IMAGE",
                    image.fileUrl(),
                    ""
            );
            saveRecord(response);
            return response;
        }
        return uploadDocx(file, fileName);
    }

    public String readText(String fileId) {
        if (!StringUtils.hasText(fileId)) {
            throw new IllegalArgumentException("文件 ID 不能为空");
        }
        Long userId = AuthContext.currentUserId();
        DocumentRecord record = documentRepository.findByFileId(userId, fileId).orElse(null);
        if (record != null && StringUtils.hasText(record.fullText())) {
            return record.fullText();
        }
        if (record == null) {
            throw new IllegalArgumentException("未找到可分析的文档");
        }

        Path pdfDir = resolveRoot().resolve(storageProperties.uploadDir()).resolve(fileId);
        if (Files.isDirectory(pdfDir)) {
            return pdfService.getContent(fileId).fullText();
        }

        Path docxDir = resolveRoot().resolve(storageProperties.documentUploadDir()).resolve(fileId);
        if (Files.isDirectory(docxDir)) {
            Path docxPath = findFirstFile(docxDir, ".docx");
            String fullText = extractDocxText(docxPath);
            if (StringUtils.hasText(fullText)) {
                documentRepository.updateFullText(userId, fileId, fullText);
            }
            return fullText;
        }

        throw new IllegalArgumentException("未找到可分析的文档");
    }

    private DocumentUploadResponse uploadDocx(MultipartFile file, String fileName) {
        String fileId = UUID.randomUUID().toString().replace("-", "");
        Path uploadPath = resolveRoot().resolve(Paths.get(storageProperties.documentUploadDir(), fileId, fileName));

        try {
            Files.createDirectories(uploadPath.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, uploadPath, StandardCopyOption.REPLACE_EXISTING);
            }
            String fullText = extractDocxText(uploadPath);
            DocumentUploadResponse response = new DocumentUploadResponse(
                    fileId,
                    fileName,
                    "DOCX",
                    "/files/" + storageProperties.documentUploadDir() + "/" + fileId + "/" + fileName,
                    fullText
            );
            saveRecord(response);
            return response;
        } catch (IOException ex) {
            throw new IllegalArgumentException("保存 DOCX 文件失败");
        }
    }

    private void saveRecord(DocumentUploadResponse response) {
        Long userId = AuthContext.currentUserId();
        documentRepository.save(
                userId,
                response.fileId(),
                response.fileName(),
                response.fileType(),
                response.fileUrl(),
                response.fullText()
        );
        profileService.recordResumeVersion(response);
        indexToKnowledgeBase(userId, response);
    }

    private void indexToKnowledgeBase(Long userId, DocumentUploadResponse response) {
        if (!StringUtils.hasText(response.fullText())) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                knowledgeRagIndexService.indexText(
                        String.valueOf(userId),
                        response.fileId(),
                        response.fileName(),
                        "resume",
                        response.fullText()
                );
            } catch (Exception ignored) {
                // 索引失败不影响上传流程
            }
        });
    }

    private String extractDocxText(Path docxPath) {
        try (InputStream inputStream = Files.newInputStream(docxPath);
             XWPFDocument document = new XWPFDocument(inputStream)) {
            Set<String> lines = new LinkedHashSet<>();
            collectLines(lines, new XWPFWordExtractor(document).getText());
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                collectLines(lines, paragraph.getText());
            }
            for (XWPFTable table : document.getTables()) {
                collectTableLines(lines, table);
            }
            for (XWPFHeader header : document.getHeaderList()) {
                for (XWPFParagraph paragraph : header.getParagraphs()) {
                    collectLines(lines, paragraph.getText());
                }
                for (XWPFTable table : header.getTables()) {
                    collectTableLines(lines, table);
                }
            }
            document.getFooterList().forEach(footer -> {
                for (XWPFParagraph paragraph : footer.getParagraphs()) {
                    collectLines(lines, paragraph.getText());
                }
                for (XWPFTable table : footer.getTables()) {
                    collectTableLines(lines, table);
                }
            });
            if (calculateTextLength(lines) < 50) {
                collectDocxXmlLines(lines, docxPath);
            }
            return String.join(System.lineSeparator(), lines);
        } catch (IOException ex) {
            throw new IllegalArgumentException("读取 DOCX 文本失败");
        }
    }

    private void collectDocxXmlLines(Set<String> lines, Path docxPath) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(docxPath))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String entryName = entry.getName();
                if (!entry.isDirectory() && entryName.startsWith("word/") && entryName.endsWith(".xml")) {
                    String xml = new String(zipInputStream.readAllBytes());
                    Matcher matcher = DOCX_TEXT_NODE_PATTERN.matcher(xml);
                    StringBuilder currentLine = new StringBuilder();
                    while (matcher.find()) {
                        String text = decodeXmlText(matcher.group(1));
                        if (!StringUtils.hasText(text)) {
                            continue;
                        }
                        appendDocxTextFragment(lines, currentLine, text);
                    }
                    flushDocxTextLine(lines, currentLine);
                }
                zipInputStream.closeEntry();
            }
        }
    }

    private void appendDocxTextFragment(Set<String> lines, StringBuilder currentLine, String fragment) {
        String clean = fragment.replace('\u00A0', ' ').trim();
        if (!StringUtils.hasText(clean)) {
            return;
        }
        if (looksLikeNewBlock(clean) && currentLine.length() > 0) {
            flushDocxTextLine(lines, currentLine);
        }
        if (currentLine.length() > 0 && shouldInsertSpace(currentLine, clean)) {
            currentLine.append(' ');
        }
        currentLine.append(clean);
        if (currentLine.length() > 160 || clean.endsWith("。") || clean.endsWith("；") || clean.endsWith(";")) {
            flushDocxTextLine(lines, currentLine);
        }
    }

    private boolean looksLikeNewBlock(String text) {
        return text.endsWith("：")
                || text.matches("^20\\d{2}.*")
                || text.contains("项目名称")
                || text.contains("软件环境")
                || text.contains("项目描述")
                || text.contains("项目职责")
                || text.contains("姓名")
                || text.contains("手机")
                || text.contains("邮箱")
                || text.contains("自我评价");
    }

    private boolean shouldInsertSpace(StringBuilder currentLine, String nextText) {
        char previous = currentLine.charAt(currentLine.length() - 1);
        char next = nextText.charAt(0);
        return Character.isLetterOrDigit(previous) && Character.isLetterOrDigit(next);
    }

    private void flushDocxTextLine(Set<String> lines, StringBuilder currentLine) {
        String line = currentLine.toString()
                .replaceAll("\\s+", " ")
                .replace("Docke r", "Docker")
                .trim();
        if (StringUtils.hasText(line)) {
            lines.add(line);
        }
        currentLine.setLength(0);
    }

    private String decodeXmlText(String text) {
        return text.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'");
    }

    private int calculateTextLength(Set<String> lines) {
        return lines.stream().mapToInt(String::length).sum();
    }

    private void collectTableLines(Set<String> lines, XWPFTable table) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    collectLines(lines, paragraph.getText());
                }
                for (XWPFTable nestedTable : cell.getTables()) {
                    collectTableLines(lines, nestedTable);
                }
            }
        }
    }

    private void collectLines(Set<String> lines, String text) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        for (String line : text.split("\\R+")) {
            String cleanLine = line.trim();
            if (StringUtils.hasText(cleanLine)) {
                lines.add(cleanLine);
            }
        }
    }

    private Path findFirstFile(Path dir, String suffix) {
        try (var files = Files.list(dir)) {
            return files
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(suffix))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("未找到文档文件"));
        } catch (IOException ex) {
            throw new IllegalArgumentException("读取文档失败");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传文档");
        }
        String fileName = file.getOriginalFilename();
        if (!StringUtils.hasText(fileName)) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        if (!(lowerName.endsWith(".pdf") || lowerName.endsWith(".docx") || isImage(lowerName))) {
            throw new IllegalArgumentException("仅支持 PDF、DOCX 和常见图片");
        }
        long maxBytes = storageProperties.maxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("文件大小不能超过 " + storageProperties.maxFileSizeMb() + "MB");
        }
    }

    private boolean isImage(String lowerName) {
        return lowerName.endsWith(".png")
                || lowerName.endsWith(".jpg")
                || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".bmp")
                || lowerName.endsWith(".webp");
    }

    private Path resolveRoot() {
        return Paths.get(storageProperties.rootDir()).toAbsolutePath().normalize();
    }
}
