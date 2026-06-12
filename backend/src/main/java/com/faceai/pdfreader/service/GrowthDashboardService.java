package com.faceai.pdfreader.service;

import com.faceai.pdfreader.model.GrowthDashboardResponse;
import com.faceai.pdfreader.model.GrowthMetricResponse;
import com.faceai.pdfreader.model.ScoreTrendPointResponse;
import com.faceai.pdfreader.model.WeakPointResponse;
import com.faceai.pdfreader.auth.AuthContext;
import com.faceai.pdfreader.repository.GrowthDashboardRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GrowthDashboardService {

    private final GrowthDashboardRepository repository;

    public GrowthDashboardService(GrowthDashboardRepository repository) {
        this.repository = repository;
    }

    public GrowthDashboardResponse overview() {
        Long userId = AuthContext.currentUserId();
        int resumeCount = repository.countResumeVersions(userId);
        int jobMatchCount = repository.countJobMatches(userId);
        int averageMatchScore = repository.averageMatchScore(userId);
        int interviewCount = repository.countInterviewSessions(userId);
        int reportCount = repository.countInterviewReports(userId);
        int averageInterviewScore = repository.averageInterviewScore(userId);
        List<ScoreTrendPointResponse> trend = repository.scoreTrend(userId);
        List<WeakPointResponse> weakPoints = buildWeakPoints(repository.lowScoreTexts(userId));

        return new GrowthDashboardResponse(
                List.of(
                        new GrowthMetricResponse("简历版本", String.valueOf(resumeCount), "累计沉淀的简历版本数量"),
                        new GrowthMetricResponse("岗位匹配", String.valueOf(jobMatchCount), "已完成的 JD 匹配分析"),
                        new GrowthMetricResponse("平均匹配分", averageMatchScore + " 分", "岗位匹配结果均分"),
                        new GrowthMetricResponse("面试训练", String.valueOf(interviewCount), "已开始的模拟面试次数"),
                        new GrowthMetricResponse("历史报告", String.valueOf(reportCount), "已优化并保存的报告数量"),
                        new GrowthMetricResponse("平均面试分", averageInterviewScore + " 分", "历史面试报告均分")
                ),
                trend,
                weakPoints,
                buildLatestAction(resumeCount, jobMatchCount, reportCount, weakPoints),
                buildSummary(resumeCount, jobMatchCount, averageMatchScore, interviewCount, averageInterviewScore)
        );
    }

    private List<WeakPointResponse> buildWeakPoints(List<String> lowScoreTexts) {
        Map<String, WeakPointResponse> templates = new LinkedHashMap<>();
        templates.put("redis", new WeakPointResponse("Redis 与缓存场景", 0, "重点补缓存穿透、击穿、雪崩、分布式锁和项目落地场景。"));
        templates.put("mysql", new WeakPointResponse("MySQL 与索引优化", 0, "重点补索引失效、Explain、事务隔离、MVCC 和慢 SQL 排查。"));
        templates.put("spring", new WeakPointResponse("Spring Boot 原理", 0, "重点补自动配置、事务失效、过滤器/拦截器/AOP 的区别。"));
        templates.put("jvm", new WeakPointResponse("JVM 与 Java 基础", 0, "重点补内存区域、GC Roots、集合、并发和线程池。"));
        templates.put("vue", new WeakPointResponse("Vue3 前端能力", 0, "重点补 Composition API、响应式、组件通信和性能优化。"));
        templates.put("project", new WeakPointResponse("项目表达与深挖", 0, "用 STAR 法补背景、职责、方案、困难、结果和复盘。"));
        templates.put("system", new WeakPointResponse("系统设计与工程化", 0, "补接口设计、鉴权、限流、异步、部署和可观测性。"));

        Map<String, Integer> counts = new LinkedHashMap<>();
        templates.keySet().forEach(key -> counts.put(key, 0));

        for (String text : lowScoreTexts) {
            String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);
            increaseIfContains(counts, "redis", lower, "redis", "缓存", "分布式锁");
            increaseIfContains(counts, "mysql", lower, "mysql", "sql", "索引", "事务", "mvcc");
            increaseIfContains(counts, "spring", lower, "spring", "事务失效", "aop", "自动配置");
            increaseIfContains(counts, "jvm", lower, "jvm", "java", "gc", "线程", "并发", "集合");
            increaseIfContains(counts, "vue", lower, "vue", "前端", "composition", "响应式");
            increaseIfContains(counts, "project", lower, "项目", "经历", "职责", "结果", "star", "表达");
            increaseIfContains(counts, "system", lower, "系统设计", "接口", "鉴权", "限流", "部署", "架构");
        }

        List<WeakPointResponse> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            WeakPointResponse template = templates.get(entry.getKey());
            result.add(new WeakPointResponse(template.name(), entry.getValue(), template.suggestion()));
        }
        return result.stream()
                .filter(item -> item.count() > 0)
                .sorted(Comparator.comparing(WeakPointResponse::count).reversed())
                .limit(5)
                .toList();
    }

    private void increaseIfContains(Map<String, Integer> counts, String key, String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                counts.put(key, counts.getOrDefault(key, 0) + 1);
                return;
            }
        }
    }

    private String buildLatestAction(
            int resumeCount,
            int jobMatchCount,
            int reportCount,
            List<WeakPointResponse> weakPoints
    ) {
        if (resumeCount == 0) {
            return "先上传一份 PDF 或 DOCX 简历，让系统建立第一版求职档案。";
        }
        if (jobMatchCount == 0) {
            return "补充一个目标岗位 JD，完成第一次岗位匹配分析。";
        }
        if (reportCount == 0) {
            return "进入模拟面试，完成一轮训练并生成第一份历史报告。";
        }
        if (!weakPoints.isEmpty()) {
            return "下一轮优先补强「" + weakPoints.get(0).name() + "」，再进行一次针对性模拟面试。";
        }
        return "继续上传新版本简历并做岗位匹配，观察匹配分和面试分是否提升。";
    }

    private String buildSummary(
            int resumeCount,
            int jobMatchCount,
            int averageMatchScore,
            int interviewCount,
            int averageInterviewScore
    ) {
        if (resumeCount == 0 && interviewCount == 0) {
            return "当前还没有足够数据。上传简历并完成一次岗位匹配后，看板会开始形成成长轨迹。";
        }
        String matchText = jobMatchCount > 0 ? "岗位匹配均分 " + averageMatchScore + " 分" : "还未进行岗位匹配";
        String interviewText = interviewCount > 0 ? "面试训练均分 " + averageInterviewScore + " 分" : "还未形成面试报告";
        return "目前已沉淀 " + resumeCount + " 个简历版本，" + matchText + "，" + interviewText + "。建议继续围绕低分题和岗位缺口做定向训练。";
    }
}
