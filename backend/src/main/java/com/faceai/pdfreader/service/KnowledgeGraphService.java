package com.faceai.pdfreader.service;

import com.faceai.pdfreader.repository.KnowledgeRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识图谱与技能树服务
 * <p>
 * 核心功能：
 * 1. 构建技能树（知识点 + 依赖关系）
 * 2. 追踪用户掌握度（EMA 算法）
 * 3. 识别薄弱知识点
 * 4. 推荐下一步学习内容
 */
@Service
public class KnowledgeGraphService {

    // EMA 平滑系数：新数据权重 0.3，历史权重 0.7
    private static final double EMA_ALPHA = 0.3;
    private static final int WEAK_THRESHOLD = 40;
    private static final int MASTERY_THRESHOLD = 70;

    private final KnowledgeRepository knowledgeRepository;

    public KnowledgeGraphService(KnowledgeRepository knowledgeRepository) {
        this.knowledgeRepository = knowledgeRepository;
    }

    /**
     * 获取技能树（不含用户掌握度）
     */
    public List<SkillTreeNode> buildSkillTree(String direction) {
        List<Map<String, Object>> points = knowledgeRepository.findAllByDirection(direction);
        List<Map<String, Object>> dependencies = knowledgeRepository.findDependencies(direction);

        // 构建节点 Map
        Map<Long, SkillTreeNode> nodeMap = new LinkedHashMap<>();
        for (Map<String, Object> point : points) {
            long id = ((Number) point.get("id")).longValue();
            String name = (String) point.get("name");
            String category = (String) point.get("category");
            String difficulty = (String) point.get("difficulty");
            String description = (String) point.get("description");
            int dependencyCount = knowledgeRepository.countDependencies(id);
            nodeMap.put(id, new SkillTreeNode(id, name, category, difficulty, description,
                    0.0, 0, dependencyCount, new ArrayList<>()));
        }

        // 构建依赖关系
        for (Map<String, Object> dep : dependencies) {
            long prerequisiteId = ((Number) dep.get("prerequisite_id")).longValue();
            long dependentId = ((Number) dep.get("dependent_id")).longValue();
            SkillTreeNode prerequisite = nodeMap.get(prerequisiteId);
            SkillTreeNode dependent = nodeMap.get(dependentId);
            if (prerequisite != null && dependent != null) {
                prerequisite.dependents().add(dependentId);
            }
        }

        return new ArrayList<>(nodeMap.values());
    }

    /**
     * 获取用户的技能树（含掌握度）
     */
    public List<SkillTreeNode> getUserSkillTree(Long userId, String direction) {
        List<SkillTreeNode> tree = buildSkillTree(direction);
        List<Map<String, Object>> masteries = knowledgeRepository.findAllMastery(userId);

        // 构建掌握度 Map
        Map<Long, Double> masteryMap = new LinkedHashMap<>();
        Map<Long, Integer> attemptMap = new LinkedHashMap<>();
        for (Map<String, Object> m : masteries) {
            long kpId = ((Number) m.get("knowledge_point_id")).longValue();
            double level = ((Number) m.get("mastery_level")).doubleValue();
            int attempts = ((Number) m.get("attempt_count")).intValue();
            masteryMap.put(kpId, level);
            attemptMap.put(kpId, attempts);
        }

        // 更新节点的掌握度
        return tree.stream()
                .map(node -> new SkillTreeNode(
                        node.id(), node.name(), node.category(), node.difficulty(),
                        node.description(),
                        masteryMap.getOrDefault(node.id(), 0.0),
                        attemptMap.getOrDefault(node.id(), 0),
                        node.dependencyCount(),
                        node.dependents()
                ))
                .toList();
    }

    /**
     * 答题后更新知识点掌握度（EMA 算法）
     * <p>
     * EMA 公式：newMastery = oldMastery * (1 - alpha) + score * alpha
     * 这样既考虑历史表现，又能反映最新状态
     *
     * @param userId          用户ID
     * @param questionId      题目ID
     * @param score           得分 0-100
     */
    public void updateMastery(Long userId, Long questionId, int score) {
        // 找到题目关联的知识点
        List<Map<String, Object>> knowledgePoints = knowledgeRepository.findKnowledgeByQuestionId(questionId);
        if (knowledgePoints.isEmpty()) return;

        boolean isCorrect = score >= 60; // 60 分以上视为掌握

        for (Map<String, Object> kp : knowledgePoints) {
            long kpId = ((Number) kp.get("id")).longValue();
            double weight = kp.get("relevance_weight") instanceof Number ?
                    ((Number) kp.get("relevance_weight")).doubleValue() : 1.0;
            double weightedScore = score * weight;

            // 获取当前掌握度
            var existing = knowledgeRepository.findMastery(userId, kpId);
            double oldMastery = existing
                    .map(m -> ((Number) m.get("mastery_level")).doubleValue())
                    .orElse(0.0);

            // EMA 更新
            double newMastery = oldMastery * (1 - EMA_ALPHA) + weightedScore * EMA_ALPHA;
            newMastery = Math.max(0, Math.min(100, newMastery));

            knowledgeRepository.upsertMastery(userId, kpId, newMastery, isCorrect);
        }
    }

    /**
     * 获取薄弱知识点（掌握度最低的）
     */
    public List<Map<String, Object>> getWeakKnowledgePoints(Long userId) {
        return knowledgeRepository.findWeakPoints(userId, 10);
    }

    /**
     * 推荐下一步学习内容
     * <p>
     * 策略：找前置已掌握但当前薄弱的知识点
     */
    public List<StudySuggestion> suggestNextStudy(Long userId, String direction) {
        List<SkillTreeNode> tree = getUserSkillTree(userId, direction);
        List<StudySuggestion> suggestions = new ArrayList<>();

        // 找出薄弱点
        List<SkillTreeNode> weakNodes = tree.stream()
                .filter(n -> n.mastery() < MASTERY_THRESHOLD && n.attemptCount() > 0)
                .sorted((a, b) -> Double.compare(a.mastery(), b.mastery()))
                .limit(5)
                .toList();

        for (SkillTreeNode weak : weakNodes) {
            // 检查前置是否已掌握
            boolean prerequisitesMet = true;
            for (Map<String, Object> prereq : knowledgeRepository.findPrerequisites(weak.id())) {
                long prereqId = ((Number) prereq.get("id")).longValue();
                var prereqMastery = knowledgeRepository.findMastery(userId, prereqId);
                double level = prereqMastery
                        .map(m -> ((Number) m.get("mastery_level")).doubleValue())
                        .orElse(0.0);
                if (level < MASTERY_THRESHOLD) {
                    prerequisitesMet = false;
                    break;
                }
            }

            String reason = prerequisitesMet ?
                    "前置知识已掌握，建议重点练习" :
                    "前置知识尚未掌握，建议先巩固基础";

            suggestions.add(new StudySuggestion(
                    weak.id(), weak.name(), weak.category(), weak.difficulty(),
                    weak.mastery(), reason
            ));
        }

        return suggestions;
    }

    /**
     * 学习建议
     */
    public record StudySuggestion(
            Long knowledgePointId,
            String name,
            String category,
            String difficulty,
            double currentMastery,
            String reason
    ) {}

    /**
     * 技能树节点
     */
    public record SkillTreeNode(
            Long id,
            String name,
            String category,
            String difficulty,
            String description,
            double mastery,
            int attemptCount,
            int dependencyCount,
            List<Long> dependents
    ) {}
}
