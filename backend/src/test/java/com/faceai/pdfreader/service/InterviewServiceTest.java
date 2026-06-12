package com.faceai.pdfreader.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faceai.pdfreader.ai.client.AiClient;
import com.faceai.pdfreader.auth.AuthContext;
import com.faceai.pdfreader.auth.AuthUser;
import com.faceai.pdfreader.model.InterviewAnswerRequest;
import com.faceai.pdfreader.model.InterviewQuestionRecord;
import com.faceai.pdfreader.model.InterviewReportResponse;
import com.faceai.pdfreader.model.InterviewSessionResponse;
import com.faceai.pdfreader.model.InterviewStartRequest;
import com.faceai.pdfreader.model.InterviewTurnRecord;
import com.faceai.pdfreader.model.InterviewTurnResponse;
import com.faceai.pdfreader.repository.InterviewQuestionRepository;
import com.faceai.pdfreader.repository.InterviewRepository;
import com.faceai.pdfreader.rag.service.InterviewRagService;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock
    private AiClient aiClient;
    @Mock
    private InterviewQuestionRepository interviewQuestionRepository;
    @Mock
    private InterviewRepository interviewRepository;
    @Mock
    private DocumentService documentService;
    @Mock
    private InterviewerService interviewerService;
    @Mock
    private GamificationService gamificationService;
    @Mock
    private EloRatingService eloRatingService;
    @Mock
    private ScoringService scoringService;
    @Mock
    private KnowledgeGraphService knowledgeGraphService;
    @Mock
    private InterviewRagService interviewRagService;

    @InjectMocks
    private InterviewService interviewService;

    @BeforeEach
    void setUp() {
        AuthContext.set(new AuthUser(1L, "testuser", "Test User"));
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void start_createsSession_withCorrectFields() {
        InterviewStartRequest request = new InterviewStartRequest(
                null, null, null, "后端开发", "标准", "Java 基础", "常规面试", "题库", null, null, null
        );
        InterviewQuestionRecord question = new InterviewQuestionRecord(
                1L, "后端开发", "Java 基础", "标准", "什么是 HashMap？", "参考答案", null, null
        );
        when(interviewQuestionRepository.findOpeningQuestion("后端开发", "Java 基础", "标准"))
                .thenReturn(Optional.of(question));

        InterviewSessionResponse response = interviewService.start(request);

        assertNotNull(response);
        assertNotNull(response.sessionId());
        assertEquals("READY", response.status());
        assertEquals("什么是 HashMap？", response.firstQuestion());
        verify(interviewRepository).save(eq(1L), anyString(), eq(request), eq(response));
    }

    @Test
    void start_usesFallbackQuestion_whenNoDbQuestion() {
        InterviewStartRequest request = new InterviewStartRequest(
                null, null, null, "前端开发", "标准", "Vue3", "常规面试", "题库", null, null, null
        );
        when(interviewQuestionRepository.findOpeningQuestion("前端开发", "Vue3", "标准"))
                .thenReturn(Optional.empty());

        InterviewSessionResponse response = interviewService.start(request);

        assertNotNull(response.firstQuestion());
        assertTrue(response.firstQuestion().contains("前端开发"));
    }

    @Test
    void submitAnswer_savesTurn_andReturnsResponse() {
        InterviewStartRequest startReq = new InterviewStartRequest(
                null, null, null, "后端开发", "标准", "Java 基础", "常规面试", "题库", null, null, null
        );
        InterviewSessionResponse session = interviewService.start(startReq);
        String sessionId = session.sessionId();

        // mock the get() call inside submitAnswer
        when(interviewRepository.findBySessionId(1L, sessionId))
                .thenReturn(Optional.of(session));
        when(interviewRepository.countTurns(sessionId)).thenReturn(0);

        InterviewAnswerRequest answerReq = new InterviewAnswerRequest("我的回答内容", 60);
        InterviewTurnResponse turnResponse = interviewService.submitAnswer(sessionId, answerReq);

        assertNotNull(turnResponse);
        assertEquals(1, turnResponse.questionNo());
        assertFalse(turnResponse.finished());
        verify(interviewRepository).saveTurn(any(InterviewTurnRecord.class));
    }

    @Test
    void generateReport_throwsWhenNoTurns() {
        InterviewStartRequest startReq = new InterviewStartRequest(
                null, null, null, "后端开发", "标准", "Java 基础", "常规面试", "题库", null, null, null
        );
        InterviewSessionResponse session = interviewService.start(startReq);
        String sessionId = session.sessionId();

        when(interviewRepository.findBySessionId(1L, sessionId))
                .thenReturn(Optional.of(session));
        when(interviewRepository.findTurnsBySessionId(sessionId))
                .thenReturn(Collections.emptyList());

        assertThrows(IllegalArgumentException.class,
                () -> interviewService.generateReport(sessionId, null));
    }

    @Test
    void generateReport_calculatesAverageScore() {
        InterviewStartRequest startReq = new InterviewStartRequest(
                null, null, null, "后端开发", "标准", "Java 基础", "常规面试", "题库", null, null, null
        );
        InterviewSessionResponse session = interviewService.start(startReq);
        String sessionId = session.sessionId();

        List<InterviewTurnRecord> turns = List.of(
                new InterviewTurnRecord(sessionId, 1, "问题1", "回答1", 30, "不错", 80),
                new InterviewTurnRecord(sessionId, 2, "问题2", "回答2", 45, "还行", 70)
        );

        when(interviewRepository.findBySessionId(1L, sessionId))
                .thenReturn(Optional.of(session));
        when(interviewRepository.findTurnsBySessionId(sessionId))
                .thenReturn(turns);

        InterviewReportResponse report = interviewService.generateReport(sessionId, "我的反思");

        assertNotNull(report);
        assertEquals(75, report.totalScore());
        assertEquals("我的反思", report.userReflection());
    }

    @Test
    void deleteReport_throwsWhenNotExists() {
        when(interviewRepository.deleteReport(1L, "nonexistent")).thenReturn(false);
        assertThrows(IllegalArgumentException.class,
                () -> interviewService.deleteReport("nonexistent"));
    }
}
