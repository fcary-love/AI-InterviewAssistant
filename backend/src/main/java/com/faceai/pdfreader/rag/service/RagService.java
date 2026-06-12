package com.faceai.pdfreader.rag.service;

import com.faceai.pdfreader.model.ChatHistoryMessage;
import com.faceai.pdfreader.model.RagIndexResponse;
import com.faceai.pdfreader.model.RagQaResponse;
import com.faceai.pdfreader.model.RedisCapabilityResponse;
import java.util.List;
import com.faceai.pdfreader.rag.skills.SinglePdfRagQaSkill;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    private final RedisCapabilityService redisCapabilityService;
    private final PdfRagIndexService pdfRagIndexService;
    private final SinglePdfRagQaSkill singlePdfRagQaSkill;

    public RagService(
            RedisCapabilityService redisCapabilityService,
            PdfRagIndexService pdfRagIndexService,
            SinglePdfRagQaSkill singlePdfRagQaSkill
    ) {
        this.redisCapabilityService = redisCapabilityService;
        this.pdfRagIndexService = pdfRagIndexService;
        this.singlePdfRagQaSkill = singlePdfRagQaSkill;
    }

    public RedisCapabilityResponse inspectRedisCapability() {
        return redisCapabilityService.inspect();
    }

    public RagIndexResponse indexPdf(String fileId) {
        return pdfRagIndexService.indexPdf(fileId);
    }

    public RagQaResponse askSinglePdf(String fileId, String question, List<ChatHistoryMessage> history) {
        return singlePdfRagQaSkill.execute(fileId, question, history);
    }
}
