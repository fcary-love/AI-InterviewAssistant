package com.faceai.pdfreader.ai.agent.tools;

import com.faceai.pdfreader.ai.agent.AgentTool;
import com.faceai.pdfreader.model.TrainingTaskResponse;
import com.faceai.pdfreader.repository.TrainingPlanRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class QueryTrainingTasksTool implements AgentTool {

    private final TrainingPlanRepository trainingPlanRepository;

    public QueryTrainingTasksTool(TrainingPlanRepository trainingPlanRepository) {
        this.trainingPlanRepository = trainingPlanRepository;
    }

    @Override
    public String name() {
        return "query_training_tasks";
    }

    @Override
    public String description() {
        return "查询用户当前的训练任务列表及其完成状态。用于了解用户的学习进度。";
    }

    @Override
    public Map<String, Object> jsonSchema() {
        return Map.of("type", "object", "properties", Map.of());
    }

    @Override
    public String execute(Map<String, Object> args, Long userId) {
        List<TrainingTaskResponse> tasks = trainingPlanRepository.listTasks(userId);
        if (tasks.isEmpty()) {
            return "该用户暂无训练任务。可以为其创建一些针对性的训练计划。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("训练任务列表（共").append(tasks.size()).append("条）：\n\n");
        for (TrainingTaskResponse task : tasks) {
            sb.append("- [").append(task.status()).append("] ");
            sb.append(task.title());
            sb.append(" | 类别: ").append(task.category());
            sb.append(" | 进度: ").append(task.finishedCount()).append("/").append(task.targetCount());
            if (!task.dueDate().isEmpty()) {
                sb.append(" | 截止: ").append(task.dueDate());
            }
            sb.append("\n");
            if (task.description() != null && !task.description().isBlank()) {
                sb.append("  描述: ").append(task.description()).append("\n");
            }
        }
        return sb.toString();
    }
}
