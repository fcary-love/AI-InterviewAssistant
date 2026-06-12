package com.faceai.pdfreader.rag.model;

public record ChunkMetadata(
        String fileId, //中文 文件名
        String fileName, // 中文 文件名
        int pageNumber, // 页码
        int chunkIndex // chunk索引
) {
}
