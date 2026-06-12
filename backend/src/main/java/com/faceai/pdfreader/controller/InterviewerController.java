package com.faceai.pdfreader.controller;

import com.faceai.pdfreader.model.record.InterviewerProfile;
import com.faceai.pdfreader.service.InterviewerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interviewers")
public class InterviewerController {

    private final InterviewerService service;

    public InterviewerController(InterviewerService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listAll() {
        List<InterviewerProfile> interviewers = service.listAll();
        return ResponseEntity.ok(Map.of("code", 200, "data", interviewers));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        InterviewerProfile interviewer = service.getById(id);
        if (interviewer == null) {
            return ResponseEntity.ok(Map.of("code", 404, "message", "面试官不存在"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "data", interviewer));
    }

    @GetMapping("/default")
    public ResponseEntity<Map<String, Object>> getDefault() {
        InterviewerProfile interviewer = service.getDefault();
        return ResponseEntity.ok(Map.of("code", 200, "data", interviewer));
    }
}
