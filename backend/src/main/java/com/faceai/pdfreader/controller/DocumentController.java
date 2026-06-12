package com.faceai.pdfreader.controller;

import com.faceai.pdfreader.model.ApiResponse;
import com.faceai.pdfreader.model.DocumentQaRequest;
import com.faceai.pdfreader.model.DocumentQaResponse;
import com.faceai.pdfreader.model.DocumentSummaryResponse;
import com.faceai.pdfreader.model.DocumentUploadResponse;
import com.faceai.pdfreader.service.DocumentAiService;
import com.faceai.pdfreader.service.DocumentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

@Validated
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentAiService documentAiService;

    public DocumentController(DocumentService documentService, DocumentAiService documentAiService) {
        this.documentService = documentService;
        this.documentAiService = documentAiService;
    }

    @PostMapping("/upload")
    public ApiResponse<DocumentUploadResponse> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(documentService.upload(file));
    }

    @PostMapping("/{fileId}/summary")
    public ApiResponse<DocumentSummaryResponse> summarize(@PathVariable @NotBlank String fileId) {
        return ApiResponse.success(documentAiService.summarize(fileId));
    }

    @PostMapping("/{fileId}/qa")
    public ApiResponse<DocumentQaResponse> answerQuestion(
            @PathVariable @NotBlank String fileId,
            @Valid @RequestBody DocumentQaRequest request
    ) {
        return ApiResponse.success(documentAiService.answerQuestion(fileId, request.question(), request.history()));
    }

    @PostMapping(value = "/{fileId}/qa/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamAnswer(
            @PathVariable @NotBlank String fileId,
            @Valid @RequestBody DocumentQaRequest request
    ) {
        return documentAiService.streamAnswer(fileId, request.question(), request.history())
                .map(token -> ServerSentEvent.<String>builder().data(token).build())
                .concatWithValues(ServerSentEvent.<String>builder().event("done").data("[DONE]").build());
    }
}
