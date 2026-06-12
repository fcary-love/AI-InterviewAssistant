package com.faceai.pdfreader.ai.agent.tools;

import com.faceai.pdfreader.ai.agent.AgentTool;
import com.faceai.pdfreader.model.TrainingTaskResponse;
import com.faceai.pdfreader.repository.TrainingPlanRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CreateTrainingTaskTool implements AgentTool {

    private final TrainingPlanRepository trainingPlanRepository;

    public CreateTrainingTaskTool(TrainingPlanRepository trainingPlanRepository) {
        this.trainingPlanRepository = trainingPlanRepository;
    }

    @Override
    public String name() {
        return "create_training_task";
    }

    @Override
    public String description() {
        return "为用户创建一条训练任务。用于制定学习计划，如刷题、复习知识点、做项目练习等。";
    }

    @Override
    public Map<String, Object> jsonSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "title", Map.of("type", "string", "description", "任务标题，如 '复习 Java 并发编程'"),
                        "description", Map.of("type", "string", "description", "任务详细描述"),
                        "taskType", Map.of("type", "string", "description", "任务类型: '刷题', '复习', '项目练习', '知识学习'"),
                        "category", Map.of("type", "string", "description", "知识类别: 'Java 基础', 'MySQL', 'Vue3', '系统设计' 等"),
                        "targetCount", Map.of("type", "integer", "description", "目标数量，如刷5道题则填5，默认1"),
                        "dueDays", Map.of("type", "integer", "description", "截止天数，如3天后截止则填3，默认7")
                ),
                "required", List.of("title")
        );
    }

    @Override
    public String execute(Map<String, Object> args, Long userId) {
        String title = (String) args.get("title");
        if (!StringUtils.hasText(title)) {
            return "任务标题不能为空。";
        }
        String description = args.containsKey("description") ? (String) args.get("description") : "";
        String taskType = args.containsKey("taskType") ? (String) args.get("taskType") : "知识学习";
        String category = args.containsKey("category") ? (String) args.get("category") : "综合";
        int targetCount = args.containsKey("targetCount") ? ((Number) args.get("targetCount")).intValue() : 1;
        int dueDays = args.containsKey("dueDays") ? ((Number) args.get("dueDays")).intValue() : 7;

        if (trainingPlanRepository.hasOpenTask(userId, title)) {
            return "已存在同名的未完成任务，无需重复创建。";
        }

        LocalDate dueDate = LocalDate.now().plusDays(Math.max(1, Math.min(dueDays, 30)));
        TrainingTaskResponse task = trainingPlanRepository.createTask(
                userId, title, description, taskType, category, targetCount, "AI 教练", dueDate
        );
        return "训练任务创建成功：\n标题: %s\n类别: %s\n类型: %s\n目标数量: %d\n截止日期: %s"
                .formatted(task.title(), task.category(), task.taskType(), task.targetCount(), task.dueDate());
    }
}
