package com.faceai.pdfreader.service;

import com.faceai.pdfreader.auth.AuthContext;
import com.faceai.pdfreader.model.TrainingGenerateResponse;
import com.faceai.pdfreader.model.TrainingPlanOverviewResponse;
import com.faceai.pdfreader.model.TrainingTaskResponse;
import com.faceai.pdfreader.repository.TrainingPlanRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TrainingPlanService {

    private final TrainingPlanRepository trainingPlanRepository;

    public TrainingPlanService(TrainingPlanRepository trainingPlanRepository) {
        this.trainingPlanRepository = trainingPlanRepository;
    }

    public TrainingPlanOverviewResponse overview() {
        return trainingPlanRepository.overview(AuthContext.currentUserId());
    }

    public List<TrainingTaskResponse> listTasks() {
        return trainingPlanRepository.listTasks(AuthContext.currentUserId());
    }

    public TrainingGenerateResponse generatePlan() {
        Long userId = AuthContext.currentUserId();
        List<TrainingTaskResponse> createdTasks = new ArrayList<>();
        List<String> weakCategories = trainingPlanRepository.weakCategories(userId, 4);
        if (weakCategories.isEmpty()) {
            createIfAbsent(userId, createdTasks, "完成一次岗位匹配", "粘贴一份目标岗位 JD，让系统输出匹配分、优势和简历缺口。", "JOB_MATCH", "岗位匹配", 1, "系统推荐", 2);
            createIfAbsent(userId, createdTasks, "刷 5 道 Java 基础题", "从题库中心选择 Java 基础题，先口述答案，再查看参考思路。", "QUESTION_PRACTICE", "Java 基础", 5, "系统推荐", 3);
            createIfAbsent(userId, createdTasks, "做一轮模拟面试", "按当前目标方向完成 5 道题，结束后保存优化报告。", "INTERVIEW", "模拟面试", 1, "系统推荐", 5);
        } else {
            int dayOffset = 2;
            for (String category : weakCategories) {
                createIfAbsent(
                        userId,
                        createdTasks,
                        "补强：" + category,
                        "围绕“" + category + "”完成 5 道题训练，并把不会讲的题加入错题复盘。",
                        "WEAK_REVIEW",
                        category,
                        5,
                        "低分题自动生成",
                        dayOffset
                );
                dayOffset += 2;
            }
            createIfAbsent(userId, createdTasks, "复盘最近一次模拟面试", "回看低分题的答案，用 STAR 或“概念-场景-项目落地”结构重写一遍。", "INTERVIEW_REVIEW", "面试复盘", 1, "低分题自动生成", dayOffset);
        }

        String message = createdTasks.isEmpty()
                ? "当前已经有可执行的训练任务，不需要重复生成。"
                : "已生成 " + createdTasks.size() + " 个训练任务。";
        return new TrainingGenerateResponse(createdTasks.size(), message, trainingPlanRepository.listTasks(userId));
    }

    public TrainingTaskResponse updateStatus(Long taskId, String status) {
        return trainingPlanRepository.updateStatus(AuthContext.currentUserId(), taskId, status);
    }

    public void deleteTask(Long taskId) {
        trainingPlanRepository.deleteTask(AuthContext.currentUserId(), taskId);
    }

    private void createIfAbsent(
            Long userId,
            List<TrainingTaskResponse> createdTasks,
            String title,
            String description,
            String taskType,
            String category,
            Integer targetCount,
            String source,
            int dueDays
    ) {
        if (trainingPlanRepository.hasOpenTask(userId, title)) {
            return;
        }
        createdTasks.add(trainingPlanRepository.createTask(
                userId,
                title,
                description,
                taskType,
                category,
                targetCount,
                source,
                LocalDate.now().plusDays(dueDays)
        ));
    }
}
