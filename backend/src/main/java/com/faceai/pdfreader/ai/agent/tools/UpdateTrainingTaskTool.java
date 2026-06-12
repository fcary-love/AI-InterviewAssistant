package com.faceai.pdfreader.ai.agent.tools;

import com.faceai.pdfreader.ai.agent.AgentTool;
import com.faceai.pdfreader.model.TrainingTaskResponse;
import com.faceai.pdfreader.repository.TrainingPlanRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class UpdateTrainingTaskTool implements AgentTool {

    private final TrainingPlanRepository trainingPlanRepository;

    public UpdateTrainingTaskTool(TrainingPlanRepository trainingPlanRepository) {
        this.trainingPlanRepository = trainingPlanRepository;
    }

    @Override
    public String name() {
        return "update_training_task";
    }

    @Override
    public String description() {
        return "更新训练任务的状态。将任务标记为进行中(DOING)或已完成(DONE)。";
    }

    @Override
    public Map<String, Object> jsonSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "taskId", Map.of("type", "integer", "description", "任务ID"),
                        "status", Map.of("type", "string", "description", "新状态: 'TODO', 'DOING', 'DONE'")
                ),
                "required", List.of("taskId", "status")
        );
    }

    @Override
    public String execute(Map<String, Object> args, Long userId) {
        long taskId = ((Number) args.get("taskId")).longValue();
        String status = (String) args.get("status");

        TrainingTaskResponse task = trainingPlanRepository.updateStatus(userId, taskId, status);
        return "任务状态更新成功：\n标题: %s\n新状态: %s\n进度: %d/%d"
                .formatted(task.title(), task.status(), task.finishedCount(), task.targetCount());
    }
}
