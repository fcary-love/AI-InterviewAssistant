package com.faceai.pdfreader.service;

import com.faceai.pdfreader.model.record.InterviewerProfile;
import com.faceai.pdfreader.repository.InterviewerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InterviewerService {

    private final InterviewerRepository repository;

    public InterviewerService(InterviewerRepository repository) {
        this.repository = repository;
    }

    public List<InterviewerProfile> listAll() {
        return repository.findAll();
    }

    public InterviewerProfile getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public InterviewerProfile getDefault() {
        return repository.findDefault().orElseGet(() -> {
            var all = repository.findAll();
            return all.isEmpty() ? null : all.get(0);
        });
    }

    /**
     * 构建面试官 system prompt，注入 AI 面试流程
     */
    public String buildInterviewerPrompt(Long interviewerId, String basePrompt) {
        InterviewerProfile interviewer = interviewerId != null
            ? repository.findById(interviewerId).orElse(null)
            : getDefault();

        if (interviewer == null) {
            return basePrompt;
        }

        return interviewer.styleDesc() + "\n\n" + basePrompt;
    }

    /**
     * 获取面试官开场白
     */
    public String getGreeting(Long interviewerId) {
        InterviewerProfile interviewer = interviewerId != null
            ? repository.findById(interviewerId).orElse(null)
            : getDefault();

        if (interviewer == null) {
            return "你好，欢迎参加今天的模拟面试。请准备好，我们即将开始。";
        }

        return interviewer.greeting();
    }
}
