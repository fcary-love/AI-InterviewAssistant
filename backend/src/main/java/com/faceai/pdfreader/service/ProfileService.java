package com.faceai.pdfreader.service;

import com.faceai.pdfreader.auth.AuthContext;
import com.faceai.pdfreader.model.DocumentUploadResponse;
import com.faceai.pdfreader.model.ProfileOverviewResponse;
import com.faceai.pdfreader.model.ResumeVersionCompareResponse;
import com.faceai.pdfreader.model.ResumeVersionDetailResponse;
import com.faceai.pdfreader.model.ResumeVersionResponse;
import com.faceai.pdfreader.repository.ProfileRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProfileService {

    private static final List<String> SKILL_KEYWORDS = List.of(
            "Java", "Spring Boot", "Spring Cloud", "MyBatis", "MyBatis-Plus",
            "MySQL", "Redis", "RabbitMQ", "Kafka", "Docker", "Vue3", "Vue",
            "Element Plus", "Linux", "Nginx", "JWT", "RESTful", "OCR", "RAG"
    );

    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public void recordResumeVersion(DocumentUploadResponse response) {
        if (response == null || !isResumeDocument(response.fileType())) {
            return;
        }
        String fullText = response.fullText() == null ? "" : response.fullText();
        profileRepository.saveResumeVersion(
                AuthContext.currentUserId(),
                response.fileId(),
                response.fileName(),
                response.fileType(),
                fullText.length(),
                extractSkillKeywords(fullText),
                buildPreview(fullText)
        );
    }

    public ProfileOverviewResponse overview() {
        Long userId = AuthContext.currentUserId();
        return new ProfileOverviewResponse(
                profileRepository.findProfile(userId),
                profileRepository.listRecentResumeVersions(userId, 5),
                profileRepository.listRecentJobMatches(userId, 5),
                profileRepository.countResumeVersions(userId),
                profileRepository.countJobMatches(userId),
                profileRepository.averageMatchScore(userId)
        );
    }

    public List<ResumeVersionResponse> listResumeVersions() {
        return profileRepository.listResumeVersions(AuthContext.currentUserId(), 30);
    }

    public ResumeVersionDetailResponse getResumeVersion(Long id) {
        return profileRepository.findResumeVersionDetail(AuthContext.currentUserId(), id)
                .orElseThrow(() -> new IllegalArgumentException("未找到简历版本"));
    }

    public ResumeVersionCompareResponse compareResumeVersions(Long leftId, Long rightId) {
        if (leftId == null || rightId == null) {
            throw new IllegalArgumentException("请选择两个简历版本");
        }
        if (leftId.equals(rightId)) {
            throw new IllegalArgumentException("请选择两个不同的简历版本");
        }

        ResumeVersionDetailResponse left = getResumeVersion(leftId);
        ResumeVersionDetailResponse right = getResumeVersion(rightId);
        Set<String> leftSkills = splitKeywords(left.skillKeywords());
        Set<String> rightSkills = splitKeywords(right.skillKeywords());

        List<String> added = new ArrayList<>(rightSkills);
        added.removeAll(leftSkills);
        List<String> removed = new ArrayList<>(leftSkills);
        removed.removeAll(rightSkills);

        int delta = safeLength(right.textLength()) - safeLength(left.textLength());
        return new ResumeVersionCompareResponse(
                left,
                right,
                delta,
                added,
                removed,
                buildCompareSummary(left, right, delta, added, removed)
        );
    }

    private boolean isResumeDocument(String fileType) {
        return "PDF".equalsIgnoreCase(fileType) || "DOCX".equalsIgnoreCase(fileType);
    }

    private String extractSkillKeywords(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String lowerText = text.toLowerCase(Locale.ROOT);
        return SKILL_KEYWORDS.stream()
                .filter(keyword -> lowerText.contains(keyword.toLowerCase(Locale.ROOT)))
                .distinct()
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private String buildPreview(String text) {
        if (!StringUtils.hasText(text)) {
            return "当前简历暂未提取到可读文本，可尝试使用 PDF 或 DOCX 文本版简历。";
        }
        String clean = text.replaceAll("\\s+", " ").trim();
        if (clean.length() <= 180) {
            return clean;
        }
        return clean.substring(0, 180) + "...";
    }

    private Set<String> splitKeywords(String keywords) {
        Set<String> result = new LinkedHashSet<>();
        if (!StringUtils.hasText(keywords)) {
            return result;
        }
        Arrays.stream(keywords.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .forEach(result::add);
        return result;
    }

    private int safeLength(Integer value) {
        return value == null ? 0 : value;
    }

    private String buildCompareSummary(
            ResumeVersionDetailResponse left,
            ResumeVersionDetailResponse right,
            int delta,
            List<String> added,
            List<String> removed
    ) {
        String trend;
        if (delta > 0) {
            trend = "新版简历比旧版多 " + delta + " 个可分析字符，内容更完整。";
        } else if (delta < 0) {
            trend = "新版简历比旧版少 " + Math.abs(delta) + " 个可分析字符，可能更精简，也可能删掉了重要经历。";
        } else {
            trend = "两版简历的可分析文本长度基本一致。";
        }

        String addedText = added.isEmpty() ? "没有发现新增技术关键词。" : "新增关键词：" + String.join("、", added) + "。";
        String removedText = removed.isEmpty() ? "没有发现移除技术关键词。" : "移除关键词：" + String.join("、", removed) + "。";
        return "对比版本 V" + left.versionNo() + " 与 V" + right.versionNo() + "：" + trend + addedText + removedText;
    }
}
