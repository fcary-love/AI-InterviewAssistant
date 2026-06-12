package com.faceai.pdfreader.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.faceai.pdfreader.model.record.UserGamification;
import com.faceai.pdfreader.repository.GamificationRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GamificationServiceTest {

    @Mock
    private GamificationRepository repository;

    @InjectMocks
    private GamificationService gamificationService;

    @Test
    void settleInterview_awardsBaseExp() {
        UserGamification gamification = new UserGamification(
                1L, 0, 1, "面试新手", 0, null, 0, 0, 0, null, null
        );
        when(repository.findByUserId(1L)).thenReturn(Optional.of(gamification));

        var result = gamificationService.settleInterview(1L, 80, 3, 80, 75, 70, 82, 68, 71);

        assertNotNull(result);
        assertTrue(result.expGained() >= 80, "Should award at least base + question exp");
        verify(repository).updateGamification(eq(1L), anyInt(), anyInt(), any(), any(), any(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void settleInterview_awardsHighScoreBonus() {
        UserGamification gamification = new UserGamification(
                1L, 0, 1, "面试新手", 0, null, 0, 0, 0, null, null
        );
        when(repository.findByUserId(1L)).thenReturn(Optional.of(gamification));

        var result = gamificationService.settleInterview(1L, 95, 3, 90, 88, 92, 85, 87, 90);

        assertTrue(result.expGained() >= 110, "Should award high score bonus");
    }

    @Test
    void settleInterview_calculatesLevelCorrectly() {
        UserGamification gamification = new UserGamification(
                1L, 8000, 9, "面试达人", 0, null, 10, 30, 85, null, null
        );
        when(repository.findByUserId(1L)).thenReturn(Optional.of(gamification));

        var result = gamificationService.settleInterview(1L, 80, 3, 80, 75, 70, 82, 68, 71);

        assertNotNull(result);
        assertTrue(result.level() >= 9, "Level should be calculated correctly");
    }

    @Test
    void getSummary_returnsCompleteData() {
        UserGamification gamification = new UserGamification(
                1L, 500, 5, "面试学徒", 3, LocalDate.now(), 5, 20, 85, null, null
        );
        when(repository.findByUserId(1L)).thenReturn(Optional.of(gamification));
        when(repository.findAllAchievements()).thenReturn(List.of());
        when(repository.findUserAchievements(1L)).thenReturn(List.of());
        when(repository.findDailyTasks(anyLong(), any())).thenReturn(List.of());

        var summary = gamificationService.getSummary(1L);

        assertNotNull(summary);
        assertEquals(500, summary.expPoints());
        assertEquals(5, summary.level());
        assertEquals("面试学徒", summary.title());
        assertEquals(3, summary.streakDays());
    }
}
