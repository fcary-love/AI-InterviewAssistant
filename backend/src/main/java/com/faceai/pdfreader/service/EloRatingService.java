package com.faceai.pdfreader.service;

import com.faceai.pdfreader.repository.EloRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 自适应难度服务 — 基于 Elo 评分算法
 * <p>
 * 核心思想：将面试视为"用户 vs 题目"的对弈。
 * 用户答对高分题 → Elo 上升 → 后续出更难的题。
 * 用户答错低分题 → Elo 下降 → 后续出更简单的题。
 */
@Service
public class EloRatingService {

    private static final double K_FACTOR = 32.0;
    private static final int COLD_START_THRESHOLD = 3;

    private final EloRepository eloRepository;

    public EloRatingService(EloRepository eloRepository) {
        this.eloRepository = eloRepository;
    }

    /**
     * 获取用户在指定方向的 Elo 评分，不存在则初始化为 1200
     */
    public double getUserRating(Long userId, String direction) {
        return eloRepository.findUserRating(userId, direction)
                .orElse(1200.0);
    }

    /**
     * 获取用户在指定方向的已答题数
     */
    public int getUserGamesPlayed(Long userId, String direction) {
        return eloRepository.findUserGamesPlayed(userId, direction);
    }

    /**
     * 获取题目的 Elo 难度，不存在则初始化为 1200
     */
    public double getQuestionDifficulty(Long questionId) {
        return eloRepository.findQuestionDifficulty(questionId)
                .orElse(1200.0);
    }

    /**
     * 答题后更新用户 Elo 和题目 Elo
     *
     * @param userId    用户ID
     * @param sessionId 面试会话ID
     * @param questionNo 题号
     * @param questionId 题目ID（可为null，使用默认难度）
     * @param score     得分 0-100
     * @param direction 方向
     * @return 更新后的用户 Elo
     */
    public double updateAfterQuestion(Long userId, String sessionId, int questionNo,
                                       Long questionId, int score, String direction) {
        double userEloBefore = getUserRating(userId, direction);
        double questionElo = questionId != null ? getQuestionDifficulty(questionId) : 1200.0;

        // Elo 公式计算
        double actualScore = score / 100.0;
        double expectedScore = 1.0 / (1.0 + Math.pow(10, (questionElo - userEloBefore) / 400.0));
        double newUserElo = userEloBefore + K_FACTOR * (actualScore - expectedScore);
        newUserElo = Math.max(100, Math.min(2400, newUserElo)); // 限制范围

        // 更新用户 Elo
        eloRepository.upsertUserRating(userId, direction, newUserElo);

        // 更新题目 Elo（反向：用户答对 → 题目应该更简单；用户答错 → 题目应该更难）
        if (questionId != null) {
            double questionExpected = 1.0 / (1.0 + Math.pow(10, (userEloBefore - questionElo) / 400.0));
            double newQuestionElo = questionElo + K_FACTOR * (questionExpected - actualScore);
            newQuestionElo = Math.max(100, Math.min(2400, newQuestionElo));
            eloRepository.upsertQuestionDifficulty(questionId, newQuestionElo, score);
        }

        // 记录轨迹
        String difficultyLabel = selectDifficultyLabel(newUserElo);
        eloRepository.saveTrajectory(userId, sessionId, questionNo,
                userEloBefore, questionElo, score, newUserElo, difficultyLabel);

        return newUserElo;
    }

    /**
     * 根据 Elo 选择难度标签
     */
    public String selectDifficultyLabel(double elo) {
        if (elo < 1100) return "简单";
        if (elo < 1300) return "标准";
        if (elo < 1500) return "困难";
        return "专家";
    }

    /**
     * 选择适合用户当前 Elo 的自适应难度
     * 冷启动阶段（前3题）返回固定难度
     */
    public String selectAdaptiveDifficulty(Long userId, String direction) {
        int gamesPlayed = getUserGamesPlayed(userId, direction);
        if (gamesPlayed < COLD_START_THRESHOLD) {
            return "标准"; // 冷启动阶段使用固定难度
        }
        double elo = getUserRating(userId, direction);
        return selectDifficultyLabel(elo);
    }

    /**
     * 获取用户的难度变化轨迹
     */
    public List<DifficultyTrajectory> getTrajectories(Long userId, String sessionId) {
        return eloRepository.findTrajectories(userId, sessionId);
    }

    /**
     * 难度轨迹记录
     */
    public record DifficultyTrajectory(
            Long id, Long userId, String sessionId, int questionNo,
            Double userEloBefore, Double questionElo, Integer score,
            Double userEloAfter, String difficultyLabel, String createdAt
    ) {}
}
