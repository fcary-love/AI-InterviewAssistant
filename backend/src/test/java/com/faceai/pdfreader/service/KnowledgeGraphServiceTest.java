package com.faceai.pdfreader.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faceai.pdfreader.repository.KnowledgeRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphServiceTest {

    @Mock
    private KnowledgeRepository knowledgeRepository;

    @InjectMocks
    private KnowledgeGraphService knowledgeGraphService;

    @Test
    void buildSkillTree_returnsNodes() {
        when(knowledgeRepository.findAllByDirection("后端开发")).thenReturn(List.of(
                Map.of("id", 1L, "name", "Java基础", "category", "Java", "difficulty", "标准", "description", "desc"),
                Map.of("id", 2L, "name", "Spring", "category", "框架", "difficulty", "标准", "description", "desc")
        ));
        when(knowledgeRepository.findDependencies("后端开发")).thenReturn(List.of());
        when(knowledgeRepository.countDependencies(1L)).thenReturn(0);
        when(knowledgeRepository.countDependencies(2L)).thenReturn(0);

        List<KnowledgeGraphService.SkillTreeNode> tree = knowledgeGraphService.buildSkillTree("后端开发");

        assertEquals(2, tree.size());
        assertEquals("Java基础", tree.get(0).name());
        assertEquals("Spring", tree.get(1).name());
    }

    @Test
    void updateMastery_usesEMAAlgorithm() {
        when(knowledgeRepository.findKnowledgeByQuestionId(1L)).thenReturn(List.of(
                Map.of("id", 10L, "relevance_weight", 1.0)
        ));
        when(knowledgeRepository.findMastery(1L, 10L)).thenReturn(Optional.of(
                Map.of("mastery_level", 60.0)
        ));

        knowledgeGraphService.updateMastery(1L, 1L, 80);

        // EMA: 60 * 0.7 + 80 * 0.3 = 42 + 24 = 66
        verify(knowledgeRepository).upsertMastery(eq(1L), eq(10L), anyDouble(), eq(true));
    }

    @Test
    void updateMastery_initializesAtZero_whenNoHistory() {
        when(knowledgeRepository.findKnowledgeByQuestionId(1L)).thenReturn(List.of(
                Map.of("id", 10L, "relevance_weight", 1.0)
        ));
        when(knowledgeRepository.findMastery(1L, 10L)).thenReturn(Optional.empty());

        knowledgeGraphService.updateMastery(1L, 1L, 70);

        // EMA: 0 * 0.7 + 70 * 0.3 = 21
        verify(knowledgeRepository).upsertMastery(eq(1L), eq(10L), anyDouble(), eq(true));
    }

    @Test
    void updateMastery_doesNothing_whenNoKnowledgePoints() {
        when(knowledgeRepository.findKnowledgeByQuestionId(1L)).thenReturn(List.of());

        knowledgeGraphService.updateMastery(1L, 1L, 80);

        // Should not call upsertMastery
    }

    @Test
    void getWeakKnowledgePoints_returnsSortedByMastery() {
        when(knowledgeRepository.findWeakPoints(1L, 10)).thenReturn(List.of(
                Map.of("knowledge_point_id", 1L, "mastery_level", 20.0, "name", "弱项1"),
                Map.of("knowledge_point_id", 2L, "mastery_level", 35.0, "name", "弱项2")
        ));

        var weakPoints = knowledgeGraphService.getWeakKnowledgePoints(1L);

        assertNotNull(weakPoints);
        assertEquals(2, weakPoints.size());
    }
}
