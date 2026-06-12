package com.faceai.pdfreader.service;

import com.faceai.pdfreader.ai.client.AiClient;
import com.faceai.pdfreader.auth.AuthContext;
import com.faceai.pdfreader.model.JobMatchResponse;
import com.faceai.pdfreader.model.JobProjectCreateRequest;
import com.faceai.pdfreader.model.JobProjectDetailResponse;
import com.faceai.pdfreader.model.JobProjectSummaryResponse;
import com.faceai.pdfreader.model.ResumeVersionDetailResponse;
import com.faceai.pdfreader.repository.JobProjectRepository;
import com.faceai.pdfreader.repository.ProfileRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JobProjectService {

    private static final int MAX_RESUME_LENGTH = 7000;
    private static final int MAX_JD_LENGTH = 5000;
    private static final List<String> KEYWORDS = List.of(
            "Java", "Spring Boot", "Spring Cloud", "MyBatis", "MyBatis-Plus", "MySQL", "Redis", "Kafka",
            "RabbitMQ", "Docker", "Kubernetes", "K8s", "Nginx", "Linux", "JVM", "多线程", "线程池",
            "并发", "分布式", "微服务", "RESTful", "Vue", "Vue3", "TypeScript", "JavaScript",
            "Element Plus", "Pinia", "Axios", "性能优化", "缓存", "索引", "事务", "安全", "鉴权",
            "JWT", "Swagger", "Git", "Maven", "JUnit", "自动化测试", "CI/CD", "云服务器"
    );

    private final AiClient aiClient;
    private final JobMatchService jobMatchService;
    private final JobProjectRepository jobProjectRepository;
    private final ProfileRepository profileRepository;

    public JobProjectService(
            AiClient aiClient,
            JobMatchService jobMatchService,
            JobProjectRepository jobProjectRepository,
            ProfileRepository profileRepository
    ) {
        this.aiClient = aiClient;
        this.jobMatchService = jobMatchService;
        this.jobProjectRepository = jobProjectRepository;
        this.profileRepository = profileRepository;
    }

    public JobProjectDetailResponse create(JobProjectCreateRequest request) {
        Long userId = AuthContext.currentUserId();
        ResumeVersionDetailResponse resume = profileRepository
                .findResumeVersionDetail(userId, request.resumeVersionId())
                .orElseThrow(() -> new IllegalArgumentException("未找到绑定的简历版本"));
        JobProjectDetailResponse created = jobProjectRepository.create(
                userId,
                request.companyName().trim(),
                request.jobTitle().trim(),
                request.jdText().trim(),
                resume.id(),
                resume.fileId()
        );
        return enrich(created, resume.fullText());
    }

    public List<JobProjectSummaryResponse> list(Integer limit) {
        return jobProjectRepository.list(AuthContext.currentUserId(), limit == null ? 30 : limit);
    }

    public JobProjectDetailResponse detail(Long id) {
        JobProjectDetailResponse project = findProject(id);
        return enrich(project, resumeText(project));
    }

    public JobProjectDetailResponse match(Long id) {
        Long userId = AuthContext.currentUserId();
        JobProjectDetailResponse project = findProject(id);
        if (!StringUtils.hasText(project.resumeFileId())) {
            throw new IllegalArgumentException("当前岗位项目没有绑定简历文件");
        }
        JobMatchResponse match = jobMatchService.analyze(project.resumeFileId(), project.jdText());
        jobProjectRepository.updateMatch(
                userId,
                id,
                match.id(),
                match.matchScore(),
                buildResumeSuggestions(match)
        );
        return detail(id);
    }

    public JobProjectDetailResponse generateTailoredResume(Long id) {
        Long userId = AuthContext.currentUserId();
        JobProjectDetailResponse project = findProject(id);
        String resumeText = resumeText(project);
        if (!StringUtils.hasText(resumeText)) {
            throw new IllegalArgumentException("当前简历版本没有可生成的文本内容");
        }
        String prompt = """
                你是一名资深简历优化顾问。请基于候选人原始简历和岗位 JD，生成一份“投递该岗位可直接参考的简历文本”。
                要求：
                1. 不要编造不存在的公司、项目或经历
                2. 可以重写表达、调整顺序、补强关键词，但必须基于原简历事实
                3. 使用中文正文格式，不要 Markdown 标题符号
                4. 输出结构包含：个人优势摘要、技能关键词、项目经历优化版、可继续补充的信息
                5. 项目经历要尽量体现岗位关键词、技术动作、结果收益

                公司：%s
                岗位：%s

                岗位 JD：
                %s

                当前简历：
                %s
                """.formatted(
                project.companyName(),
                project.jobTitle(),
                limit(project.jdText(), MAX_JD_LENGTH),
                limit(resumeText, MAX_RESUME_LENGTH)
        );
        String tailored = aiClient.chat(prompt);
        jobProjectRepository.updateTailoredResume(userId, id, tailored);
        return detail(id);
    }

    public JobProjectDetailResponse updateStatus(Long id, String status) {
        validateStatus(status);
        jobProjectRepository.updateStatus(AuthContext.currentUserId(), id, status);
        return detail(id);
    }

    private JobProjectDetailResponse findProject(Long id) {
        return jobProjectRepository.findDetail(AuthContext.currentUserId(), id)
                .orElseThrow(() -> new IllegalArgumentException("岗位项目不存在"));
    }

    private JobProjectDetailResponse enrich(JobProjectDetailResponse project, String resumeText) {
        List<String> jdKeywords = extractKeywords(project.jdText());
        List<String> resumeKeywords = extractKeywords(resumeText);
        Set<String> resumeSet = new LinkedHashSet<>(resumeKeywords);
        List<String> missing = jdKeywords.stream()
                .filter(keyword -> !resumeSet.contains(keyword))
                .toList();
        return new JobProjectDetailResponse(
                project.id(),
                project.companyName(),
                project.jobTitle(),
                project.jdText(),
                project.resumeVersionId(),
                project.resumeFileId(),
                project.resumeFileName(),
                project.resumeVersionNo(),
                project.matchAnalysisId(),
                project.matchScore(),
                project.status(),
                project.resumeSuggestions(),
                project.tailoredResumeText(),
                project.finalConclusion(),
                project.createdAt(),
                project.updatedAt(),
                jdKeywords,
                resumeKeywords,
                missing
        );
    }

    private String resumeText(JobProjectDetailResponse project) {
        if (project.resumeVersionId() == null) {
            return "";
        }
        return profileRepository
                .findResumeVersionDetail(AuthContext.currentUserId(), project.resumeVersionId())
                .map(ResumeVersionDetailResponse::fullText)
                .orElse("");
    }

    private String buildResumeSuggestions(JobMatchResponse match) {
        List<String> sections = new ArrayList<>();
        if (match.matchScore() != null) {
            sections.add("当前岗位匹配分：" + match.matchScore() + " 分。");
        }
        if (!match.missingKeywords().isEmpty()) {
            sections.add("优先补强关键词：" + String.join("、", match.missingKeywords()) + "。");
        }
        if (!match.rewriteSuggestions().isEmpty()) {
            sections.add("简历修改建议：" + String.join("\n", match.rewriteSuggestions()));
        }
        if (StringUtils.hasText(match.analysisContent())) {
            sections.add(match.analysisContent());
        }
        return String.join("\n\n", sections);
    }

    private List<String> extractKeywords(String text) {
        String lowerText = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return KEYWORDS.stream()
                .filter(keyword -> lowerText.contains(keyword.toLowerCase(Locale.ROOT)))
                .distinct()
                .toList();
    }

    private void validateStatus(String status) {
        if (!List.of("待分析", "已匹配", "已优化", "已面试", "已复盘").contains(status)) {
            throw new IllegalArgumentException("不支持的岗位项目状态");
        }
    }

    private String limit(String text, int maxLength) {
        String clean = text == null ? "" : text.trim();
        if (clean.length() <= maxLength) {
            return clean;
        }
        return clean.substring(0, maxLength) + "\n\n[内容过长，已截断]";
    }
}
