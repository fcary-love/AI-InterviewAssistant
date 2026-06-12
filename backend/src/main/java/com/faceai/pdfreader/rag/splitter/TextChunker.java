package com.faceai.pdfreader.rag.splitter;

import com.faceai.pdfreader.config.RagProperties;
import com.faceai.pdfreader.model.PageText;
import com.faceai.pdfreader.rag.model.ChunkMetadata;
import com.faceai.pdfreader.rag.model.ChunkedText;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TextChunker {

    private final RagProperties ragProperties;

    public TextChunker(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    public List<ChunkedText> split(String fileId, String fileName, List<PageText> pageTexts) {
        List<ChunkedText> chunks = new ArrayList<>();
        int globalChunkIndex = 1;
        for (PageText pageText : pageTexts) {
            String text = normalize(pageText.text());
            if (!StringUtils.hasText(text)) {
                continue;
            }
            int start = 0;
            while (start < text.length()) {
                int end = Math.min(start + ragProperties.chunkSize(), text.length());
                String chunkText = text.substring(start, end).trim();
                if (!chunkText.isBlank()) {
                    ChunkMetadata metadata = new ChunkMetadata(fileId, fileName, pageText.pageNumber(), globalChunkIndex);
                    chunks.add(new ChunkedText(
                            fileId + "-p" + pageText.pageNumber() + "-c" + globalChunkIndex,
                            chunkText,
                            metadata
                    ));
                    globalChunkIndex++;
                }
                if (end >= text.length()) {
                    break;
                }
                start = Math.max(end - ragProperties.chunkOverlap(), start + 1);
            }
        }
        return chunks;
    }

    private String normalize(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.replace("\r", "")
                .replaceAll("\\n{3,}", "\n\n")
                .replaceAll("[ \\t]{2,}", " ")
                .trim();
    }
}
