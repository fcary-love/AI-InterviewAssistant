package com.faceai.pdfreader.controller;

import com.faceai.pdfreader.model.ApiResponse;
import com.faceai.pdfreader.model.AiQaRequest;
import com.faceai.pdfreader.model.AiQaResponse;
import com.faceai.pdfreader.model.AiSummaryResponse;
import com.faceai.pdfreader.model.ImageContentResponse;
import com.faceai.pdfreader.model.ImageDescriptionRequest;
import com.faceai.pdfreader.model.ImageDescriptionResponse;
import com.faceai.pdfreader.model.OcrResponse;
import com.faceai.pdfreader.model.PdfContentResponse;
import com.faceai.pdfreader.model.RagIndexResponse;
import com.faceai.pdfreader.model.RagQaRequest;
import com.faceai.pdfreader.model.RagQaResponse;
import com.faceai.pdfreader.model.RedisCapabilityResponse;
import com.faceai.pdfreader.model.UploadResponse;
import com.faceai.pdfreader.rag.service.RagService;
import com.faceai.pdfreader.service.AiService;
import com.faceai.pdfreader.service.ImageService;
import com.faceai.pdfreader.service.OcrService;
import com.faceai.pdfreader.service.PdfService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    private final PdfService pdfService;
    private final ImageService imageService;
    private final OcrService ocrService;
    private final AiService aiService;
    private final RagService ragService;

    public PdfController(
            PdfService pdfService,
            ImageService imageService,
            OcrService ocrService,
            AiService aiService,
            RagService ragService
    ) {
        this.pdfService = pdfService;
        this.imageService = imageService;
        this.ocrService = ocrService;
        this.aiService = aiService;
        this.ragService = ragService;
    }

    @PostMapping("/upload")
    public ApiResponse<UploadResponse> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(pdfService.upload(file));
    }

    @PostMapping("/image/upload")
    public ApiResponse<ImageContentResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(imageService.upload(file));
    }

    @GetMapping("/{fileId}")
    public ApiResponse<PdfContentResponse> getContent(@PathVariable @NotBlank String fileId) {
        return ApiResponse.success(pdfService.getContent(fileId));
    }

    @GetMapping("/image/{fileId}")
    public ApiResponse<ImageContentResponse> getImageContent(@PathVariable @NotBlank String fileId) {
        return ApiResponse.success(imageService.getContent(fileId));
    }

    @PostMapping("/{fileId}/ocr")
    public ApiResponse<OcrResponse> runOcr(@PathVariable @NotBlank String fileId) {
        return ApiResponse.success(ocrService.extractTextFromPdf(fileId));
    }

    @PostMapping("/image/{fileId}/ocr")
    public ApiResponse<OcrResponse> runImageOcr(@PathVariable @NotBlank String fileId) {
        return ApiResponse.success(ocrService.extractTextFromImage(fileId));
    }

    @PostMapping("/{fileId}/summary")
    public ApiResponse<AiSummaryResponse> summarize(@PathVariable @NotBlank String fileId) {
        return ApiResponse.success(aiService.summarize(fileId));
    }

    @PostMapping("/{fileId}/qa")
    public ApiResponse<AiQaResponse> answerQuestion(
            @PathVariable @NotBlank String fileId,
            @Valid @RequestBody AiQaRequest request
    ) {
        return ApiResponse.success(aiService.answerQuestion(fileId, request.question(), request.history()));
    }

    @PostMapping("/{fileId}/images/describe")
    public ApiResponse<ImageDescriptionResponse> describeImage(
            @PathVariable @NotBlank String fileId,
            @Valid @RequestBody ImageDescriptionRequest request
    ) {
        return ApiResponse.success(aiService.describeImage(fileId, request.imageUrl()));
    }

    @PostMapping("/image/{fileId}/describe")
    public ApiResponse<ImageDescriptionResponse> describeUploadedImage(@PathVariable @NotBlank String fileId) {
        return ApiResponse.success(aiService.describeUploadedImage(fileId));
    }

    @GetMapping("/rag/redis-capability")
    public ApiResponse<RedisCapabilityResponse> inspectRedisCapability() {
        return ApiResponse.success(ragService.inspectRedisCapability());
    }

    @PostMapping("/{fileId}/rag/index")
    public ApiResponse<RagIndexResponse> buildRagIndex(@PathVariable @NotBlank String fileId) {
        return ApiResponse.success(ragService.indexPdf(fileId));
    }

    @PostMapping("/{fileId}/rag/qa")
    public ApiResponse<RagQaResponse> ragQa(
            @PathVariable @NotBlank String fileId,
            @Valid @RequestBody RagQaRequest request
    ) {
        return ApiResponse.success(ragService.askSinglePdf(fileId, request.question(), request.history()));
    }
}
