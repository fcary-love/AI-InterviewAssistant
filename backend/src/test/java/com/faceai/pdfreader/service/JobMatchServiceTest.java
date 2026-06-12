package com.faceai.pdfreader.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.faceai.pdfreader.ai.client.AiClient;
import com.faceai.pdfreader.auth.AuthContext;
import com.faceai.pdfreader.auth.AuthUser;
import com.faceai.pdfreader.model.JobMatchResponse;
import com.faceai.pdfreader.repository.JobMatchRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobMatchServiceTest {

    @Mock
    private AiClient aiClient;
    @Mock
    private DocumentService documentService;
    @Mock
    private JobMatchRepository jobMatchRepository;

    @InjectMocks
    private JobMatchService jobMatchService;

    @BeforeEach
    void setUp() {
        AuthContext.set(new AuthUser(1L, "testuser", "Test User"));
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void analyze_extractsKeywords_andCallsAi() {
        when(documentService.readText("file123"))
                .thenReturn("Java Spring Boot MySQL Redis 开发经验");
        when(aiClient.chat(anyString()))
                .thenReturn("匹配分：75\n岗位核心要求：需要熟悉 Java 和 Spring Boot\n匹配优势：候选人具备 Java 和 Spring Boot 经验\n明显缺口：缺少微服务经验\n简历修改建议：补充微服务项目\n可替换简历表述：使用 Spring Cloud 构建微服务\n面试准备建议：准备微服务相关问题");
        when(jobMatchRepository.save(eq(1L), eq("file123"), anyString(), eq(75), anyString()))
                .thenReturn(new JobMatchResponse(1L, "file123", 75, "分析内容", "2026-05-28 10:00:00", List.of(), List.of(), List.of(), List.of(), List.of()));

        JobMatchResponse response = jobMatchService.analyze("file123", "要求熟悉 Java 和 Spring Boot");

        assertNotNull(response);
        assertEquals(75, response.matchScore());
    }

    @Test
    void analyze_throwsWhenNoResumeText() {
        when(documentService.readText("file123")).thenReturn("");

        assertThrows(IllegalArgumentException.class,
                () -> jobMatchService.analyze("file123", "JD 内容"));
    }

    @Test
    void analyze_throwsWhenNoJdText() {
        when(documentService.readText("file123")).thenReturn("Java 开发经验");

        assertThrows(IllegalArgumentException.class,
                () -> jobMatchService.analyze("file123", ""));
    }

    @Test
    void analyze_throwsWhenNullJdText() {
        when(documentService.readText("file123")).thenReturn("Java 开发经验");

        assertThrows(IllegalArgumentException.class,
                () -> jobMatchService.analyze("file123", null));
    }
}
