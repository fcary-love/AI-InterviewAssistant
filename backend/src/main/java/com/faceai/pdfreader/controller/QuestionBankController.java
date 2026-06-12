package com.faceai.pdfreader.controller;

import com.faceai.pdfreader.ai.client.AiClient;
import com.faceai.pdfreader.auth.AuthContext;
import com.faceai.pdfreader.model.ApiResponse;
import com.faceai.pdfreader.model.QuestionBankItemResponse;
import com.faceai.pdfreader.model.QuestionBankOverviewResponse;
import com.faceai.pdfreader.model.QuestionExplainRequest;
import com.faceai.pdfreader.model.QuestionExplainResponse;
import com.faceai.pdfreader.model.WrongQuestionResponse;
import com.faceai.pdfreader.rag.service.InterviewRagService;
import com.faceai.pdfreader.repository.InterviewQuestionRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/questions")
public class QuestionBankController {

    private final InterviewQuestionRepository interviewQuestionRepository;
    private final AiClient aiClient;
    private final InterviewRagService interviewRagService;

    public QuestionBankController(InterviewQuestionRepository interviewQuestionRepository,
                                   AiClient aiClient,
                                   InterviewRagService interviewRagService) {
        this.interviewQuestionRepository = interviewQuestionRepository;
        this.aiClient = aiClient;
        this.interviewRagService = interviewRagService;
    }

    @GetMapping("/overview")
    public ApiResponse<QuestionBankOverviewResponse> overview() {
        return ApiResponse.success(interviewQuestionRepository.overview());
    }

    @GetMapping
    public ApiResponse<List<QuestionBankItemResponse>> list(
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "60") Integer limit
    ) {
        return ApiResponse.success(interviewQuestionRepository.listQuestions(
                direction,
                category,
                difficulty,
                keyword,
                limit
        ));
    }

    @GetMapping("/wrong")
    public ApiResponse<List<WrongQuestionResponse>> wrongQuestions(
            @RequestParam(defaultValue = "70") Integer maxScore,
            @RequestParam(defaultValue = "60") Integer limit
    ) {
        return ApiResponse.success(interviewQuestionRepository.listWrongQuestions(AuthContext.currentUserId(), maxScore, limit));
    }

    @PostMapping("/wrong/{sessionId}/{questionNo}/review")
    public ApiResponse<Void> markReviewed(
            @PathVariable String sessionId,
            @PathVariable Integer questionNo
    ) {
        interviewQuestionRepository.markReviewed(sessionId, questionNo);
        return ApiResponse.success(null);
    }

    @PostMapping("/explain")
    public ApiResponse<QuestionExplainResponse> explain(@Valid @RequestBody QuestionExplainRequest request) {
        aiClient.validateConfiguration();
        String prompt = """
                你是一名面试题讲解教练。请讲解下面这道题，要求：
                1. 先说明这题考察什么
                2. 给出面试时推荐的回答结构
                3. 给一版自然、可直接口述的参考回答
                4. 提醒 2-3 个容易丢分的点
                5. 使用自然中文正文，不要使用 Markdown 标题、星号、井号或代码块
                6. 控制在 800 字以内

                题目：
                %s
                """.formatted(request.question());
        return ApiResponse.success(new QuestionExplainResponse(aiClient.chat(prompt)));
    }

    @PostMapping("/reindex")
    public ApiResponse<Map<String, Object>> reindex() {
        int indexed = interviewRagService.indexQuestionBank();
        return ApiResponse.success(Map.of(
                "indexed", indexed,
                "message", "题库索引完成，共索引 %d 道题目".formatted(indexed)
        ));
    }

    @GetMapping("/rag-status")
    public ApiResponse<Map<String, Object>> ragStatus() {
        return ApiResponse.success(interviewRagService.getIndexStatus());
    }
}
