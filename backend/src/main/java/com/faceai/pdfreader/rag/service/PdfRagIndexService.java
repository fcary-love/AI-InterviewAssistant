package com.faceai.pdfreader.rag.service;

import com.faceai.pdfreader.config.RagProperties;
import com.faceai.pdfreader.model.OcrResponse;
import com.faceai.pdfreader.model.PageText;
import com.faceai.pdfreader.model.PdfContentResponse;
import com.faceai.pdfreader.model.RagIndexResponse;
import com.faceai.pdfreader.rag.model.ChunkedText;
import com.faceai.pdfreader.rag.splitter.TextChunker;
import com.faceai.pdfreader.service.OcrService;
import com.faceai.pdfreader.service.PdfService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PdfRagIndexService {

    private final RagProperties ragProperties;
    private final PdfService pdfService; // 用于获取 PDF 内容
    private final OcrService ocrService; // 用于从 PDF 中提取文本    
    private final TextChunker textChunker; // 用于将文本分割成块
    private final ObjectProvider<VectorStore> vectorStoreProvider; // 用于按需存储向量表示

    public PdfRagIndexService(
            RagProperties ragProperties,
            PdfService pdfService,
            OcrService ocrService,
            TextChunker textChunker,
            ObjectProvider<VectorStore> vectorStoreProvider
    ) {
        this.ragProperties = ragProperties;
        this.pdfService = pdfService;
        this.ocrService = ocrService;
        this.textChunker = textChunker;
        this.vectorStoreProvider = vectorStoreProvider;
    }

    public RagIndexResponse indexPdf(String fileId) {
        ensureEnabled();
        PdfContentResponse content = pdfService.getContent(fileId);
        List<PageText> pageTexts = preparePageTexts(content);
        List<ChunkedText> chunks = textChunker.split(content.fileId(), content.fileName(), pageTexts);
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("当前 PDF 没有可索引的文本内容");
        }

        List<Document> documents = new ArrayList<>();
        for (ChunkedText chunk : chunks) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("fileId", chunk.metadata().fileId());
            metadata.put("fileName", chunk.metadata().fileName());
            metadata.put("pageNumber", chunk.metadata().pageNumber());
            metadata.put("chunkIndex", chunk.metadata().chunkIndex());
            documents.add(new Document(chunk.text(), metadata));
        }

        vectorStore().add(documents);
        return new RagIndexResponse(content.fileId(), content.fileName(), documents.size(), "RAG 索引构建完成");
    }

    private VectorStore vectorStore() {
        try {
            return vectorStoreProvider.getObject();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Redis 向量库不可用，请先启动 Redis Stack 后再使用 RAG 功能");
        }
    }

    private List<PageText> preparePageTexts(PdfContentResponse content) {
        boolean hasDirectText = content.pageTexts().stream().anyMatch(page -> StringUtils.hasText(page.text()));
        if (hasDirectText) {
            return content.pageTexts();
        }
        OcrResponse ocrResponse = ocrService.extractTextFromPdf(content.fileId());
        return ocrResponse.pageResults().stream()
                .map(page -> new PageText(page.pageNumber(), page.text()))
                .toList();
    }

    private void ensureEnabled() {
        if (!ragProperties.enabled()) {
            throw new IllegalArgumentException("RAG 功能未开启，请在 application.yml 中设置 app.rag.enabled=true");
        }
    }
}
