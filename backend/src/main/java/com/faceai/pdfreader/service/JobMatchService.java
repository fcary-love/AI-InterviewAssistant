package com.faceai.pdfreader.service;

import com.faceai.pdfreader.ai.client.AiClient;
import com.faceai.pdfreader.auth.AuthContext;
import com.faceai.pdfreader.model.JobMatchResponse;
import com.faceai.pdfreader.model.JobMatchDetailResponse;
import com.faceai.pdfreader.model.JobMatchHistoryResponse;
import com.faceai.pdfreader.rag.service.KnowledgeRagIndexService;
import com.faceai.pdfreader.repository.JobMatchRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JobMatchService {

    private static final int MAX_RESUME_LENGTH = 7000;
    private static final int MAX_JD_LENGTH = 5000;
    private static final Pattern SCORE_PATTERN = Pattern.compile("(?:匹配分|匹配度|score)\\s*[:：]\\s*(\\d{1,3})", Pattern.CASE_INSENSITIVE);
    private static final Pattern LINE_SPLIT_PATTERN = Pattern.compile("[\\r\\n；;。]+");
    private static final List<String> TECH_KEYWORDS = List.of(
            "Java", "Spring Boot", "Spring Cloud", "MyBatis", "MyBatis-Plus", "MySQL", "Redis", "Kafka",
            "RabbitMQ", "Docker", "Kubernetes", "K8s", "Nginx", "Linux", "JVM", "多线程", "线程池",
            "并发", "分布式", "微服务", "接口", "RESTful", "Vue", "Vue3", "TypeScript", "JavaScript",
            "Element Plus", "Pinia", "Axios", "性能优化", "缓存", "索引", "事务", "安全", "鉴权",
            "JWT", "Swagger", "Git", "Maven", "JUnit", "自动化测试", "CI/CD", "云服务器"
    );
    private static final List<String> REQUIREMENT_MARKERS = List.of(
            "要求", "熟悉", "掌握", "负责", "经验", "优先", "具备", "理解", "参与", "开发", "设计", "优化"
    );

    private final AiClient aiClient;
    private final DocumentService documentService;
    private final JobMatchRepository jobMatchRepository;
    private final KnowledgeRagIndexService knowledgeRagIndexService;

    public JobMatchService(
            AiClient aiClient,
            DocumentService documentService,
            JobMatchRepository jobMatchRepository,
            KnowledgeRagIndexService knowledgeRagIndexService
    ) {
        this.aiClient = aiClient;
        this.documentService = documentService;
        this.jobMatchRepository = jobMatchRepository;
        this.knowledgeRagIndexService = knowledgeRagIndexService;
    }

    public JobMatchResponse analyze(String resumeFileId, String jdText) {
        String resumeText = documentService.readText(resumeFileId);
        if (!StringUtils.hasText(resumeText)) {
            throw new IllegalArgumentException("当前简历没有可分析的文本内容，请换一份可读取的简历");
        }
        String cleanJd = normalize(jdText, MAX_JD_LENGTH);
        if (!StringUtils.hasText(cleanJd)) {
            throw new IllegalArgumentException("请先补充岗位 JD");
        }
        JobMatchInsight insight = buildInsight(resumeText, cleanJd);
        String prompt = """
                你是一名资深招聘面试顾问。请根据候选人简历和岗位 JD，生成中文岗位匹配分析。
                请使用自然正文格式，不要使用 Markdown 井号、星号、代码块或表情符号。
                分析时优先参考我提前提取出的 JD 结构化信息，但你需要结合简历文本做判断，不要机械照抄。
                请严格包含以下结构：

                匹配分：0-100 的整数

                岗位核心要求：
                提炼 JD 中最重要的 5-8 个要求。

                匹配优势：
                说明简历中哪些经历、项目、技能能匹配岗位要求。

                明显缺口：
                说明简历中缺少、表达不清、或与 JD 不匹配的地方。

                简历修改建议：
                给出可以直接改简历的建议，重点包括项目描述、技能关键词、量化结果、岗位关键词补齐。

                可替换简历表述：
                给出 3-5 条更适合投递这个岗位的简历 bullet 示例。

                面试准备建议：
                根据 JD 和简历，列出候选人最可能被追问的方向。

                JD 结构化信息：
                核心要求：%s
                JD 技术关键词：%s
                简历已覆盖关键词：%s
                简历缺口关键词：%s

                候选人简历：
                %s

                岗位 JD：
                %s
                """.formatted(
                String.join("；", insight.coreRequirements()),
                String.join("，", insight.jdKeywords()),
                String.join("，", insight.matchedKeywords()),
                String.join("，", insight.missingKeywords()),
                normalize(resumeText, MAX_RESUME_LENGTH),
                cleanJd
        );
        String analysis = aiClient.chat(prompt);
        Integer score = parseScore(analysis);
        if (score == null) {
            score = estimateScore(insight);
        }
        JobMatchResponse saved = jobMatchRepository.save(AuthContext.currentUserId(), resumeFileId, cleanJd, score, analysis);
        indexJdToKnowledgeBase(AuthContext.currentUserId(), cleanJd);
        return new JobMatchResponse(
                saved.id(),
                saved.resumeFileId(),
                saved.matchScore(),
                saved.analysisContent(),
                saved.createdAt(),
                insight.coreRequirements(),
                insight.matchedKeywords(),
                insight.missingKeywords(),
                insight.rewriteSuggestions(),
                insight.interviewFocus()
        );
    }

    public List<JobMatchHistoryResponse> listHistory(Integer limit) {
        return jobMatchRepository.list(AuthContext.currentUserId(), limit == null ? 30 : limit);
    }

    public JobMatchDetailResponse detail(Long id) {
        JobMatchDetailResponse detail = jobMatchRepository
                .findDetail(AuthContext.currentUserId(), id)
                .orElseThrow(() -> new IllegalArgumentException("岗位匹配记录不存在"));
        String resumeText = "";
        try {
            resumeText = documentService.readText(detail.resumeFileId());
        } catch (Exception ignored) {
            resumeText = "";
        }
        JobMatchInsight insight = buildInsight(resumeText, detail.jdText());
        return new JobMatchDetailResponse(
                detail.id(),
                detail.resumeFileId(),
                detail.resumeFileName(),
                detail.matchScore(),
                detail.jdText(),
                detail.analysisContent(),
                detail.createdAt(),
                insight.coreRequirements(),
                insight.matchedKeywords(),
                insight.missingKeywords(),
                insight.rewriteSuggestions(),
                insight.interviewFocus()
        );
    }

    private JobMatchInsight buildInsight(String resumeText, String jdText) {
        List<String> jdKeywords = extractKeywords(jdText);
        List<String> resumeKeywords = extractKeywords(resumeText);
        Set<String> resumeKeywordSet = new LinkedHashSet<>(resumeKeywords);
        List<String> matchedKeywords = jdKeywords.stream()
                .filter(resumeKeywordSet::contains)
                .limit(12)
                .toList();
        List<String> missingKeywords = jdKeywords.stream()
                .filter(keyword -> !resumeKeywordSet.contains(keyword))
                .limit(12)
                .toList();
        List<String> coreRequirements = extractCoreRequirements(jdText, jdKeywords);
        return new JobMatchInsight(
                coreRequirements,
                jdKeywords,
                matchedKeywords,
                missingKeywords,
                buildRewriteSuggestions(matchedKeywords, missingKeywords),
                buildInterviewFocus(coreRequirements, missingKeywords)
        );
    }

    private List<String> extractKeywords(String text) {
        String lowerText = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return TECH_KEYWORDS.stream()
                .filter(keyword -> lowerText.contains(keyword.toLowerCase(Locale.ROOT)))
                .distinct()
                .toList();
    }

    private List<String> extractCoreRequirements(String jdText, List<String> jdKeywords) {
        List<String> requirements = new ArrayList<>();
        for (String rawLine : LINE_SPLIT_PATTERN.split(jdText == null ? "" : jdText)) {
            String line = rawLine.replaceAll("\\s+", " ").trim();
            if (line.length() < 8 || line.length() > 130) {
                continue;
            }
            if (looksLikeRequirement(line)) {
                requirements.add(line);
            }
            if (requirements.size() >= 8) {
                break;
            }
        }
        if (!requirements.isEmpty()) {
            return requirements;
        }
        return jdKeywords.stream()
                .limit(6)
                .map(keyword -> "岗位需要候选人具备 " + keyword + " 相关能力或项目经验")
                .toList();
    }

    private boolean looksLikeRequirement(String line) {
        return REQUIREMENT_MARKERS.stream().anyMatch(line::contains);
    }

    private List<String> buildRewriteSuggestions(List<String> matchedKeywords, List<String> missingKeywords) {
        List<String> suggestions = new ArrayList<>();
        if (!matchedKeywords.isEmpty()) {
            suggestions.add("把已具备的 " + String.join("、", matchedKeywords.stream().limit(5).toList()) + " 写进项目职责和技术栈，不要只堆在技能清单里。");
        }
        if (!missingKeywords.isEmpty()) {
            suggestions.add("针对 JD 缺口补齐 " + String.join("、", missingKeywords.stream().limit(5).toList()) + " 的应用场景、学习证明或替代经验。");
        }
        suggestions.add("每个核心项目至少补一条量化结果，例如响应时间、并发量、数据量、缺陷率或交付周期。");
        suggestions.add("把项目描述改成“背景问题、你的动作、技术取舍、结果收益”的结构，更适合面试追问。");
        return suggestions;
    }

    private List<String> buildInterviewFocus(List<String> coreRequirements, List<String> missingKeywords) {
        List<String> focus = new ArrayList<>();
        coreRequirements.stream()
                .limit(3)
                .map(requirement -> "围绕 JD 要求追问：" + requirement)
                .forEach(focus::add);
        missingKeywords.stream()
                .limit(4)
                .map(keyword -> "补强缺口关键词：" + keyword + " 的原理、场景和项目落地")
                .forEach(focus::add);
        if (focus.isEmpty()) {
            focus.add("准备一个最能证明岗位匹配度的项目，讲清楚背景、职责、方案和结果。");
        }
        return focus;
    }

    private int estimateScore(JobMatchInsight insight) {
        int total = Math.max(1, insight.jdKeywords().size());
        int matched = insight.matchedKeywords().size();
        int score = 45 + Math.round(matched * 45f / total);
        return Math.min(92, Math.max(35, score));
    }

    private String normalize(String text, int maxLength) {
        String cleanText = text == null ? "" : text.trim();
        if (cleanText.length() <= maxLength) {
            return cleanText;
        }
        return cleanText.substring(0, maxLength) + "\n\n[内容过长，已截断前 " + maxLength + " 个字符用于分析]";
    }

    private Integer parseScore(String analysis) {
        Matcher matcher = SCORE_PATTERN.matcher(analysis == null ? "" : analysis);
        if (!matcher.find()) {
            return null;
        }
        int score = Integer.parseInt(matcher.group(1));
        if (score < 0) {
            return 0;
        }
        return Math.min(score, 100);
    }

    private void indexJdToKnowledgeBase(Long userId, String jdText) {
        CompletableFuture.runAsync(() -> {
            try {
                String jdFileId = "jd-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                knowledgeRagIndexService.indexText(
                        String.valueOf(userId),
                        jdFileId,
                        "JD-" + jdText.substring(0, Math.min(30, jdText.length())),
                        "jd",
                        jdText
                );
            } catch (Exception ignored) {
                // 索引失败不影响分析流程
            }
        });
    }

    private record JobMatchInsight(
            List<String> coreRequirements,
            List<String> jdKeywords,
            List<String> matchedKeywords,
            List<String> missingKeywords,
            List<String> rewriteSuggestions,
            List<String> interviewFocus
    ) {
    }
}
