package com.example.quiz.service;

import com.example.quiz.dto.ScoreEntry;
import com.example.quiz.entity.ScoreEntity;
import com.example.quiz.repository.ScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final ScoreRepository scoreRepository;

    // Метод получения списка лидеров
    public List<ScoreEntry> getLeaderboard(String category) {
        // 1. Достаем из БД отсортированные данные
        List<ScoreEntity> entities = scoreRepository.findByCategoryOrderByScoreDescTimeSecondsAsc(category);

        // 2. Ограничиваем топ-15 (если нужно) и конвертируем в DTO для отправки на фронтенд
        return entities.stream()
                .limit(15)
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Получение всех досок сразу (для отображения на странице лидерборда)
    // Внимание: Этот метод может быть тяжелым, если данных много, но для начала сойдет.
    public java.util.Map<String, List<ScoreEntry>> getAllLeaderboards() {
        java.util.Map<String, List<ScoreEntry>> map = new java.util.HashMap<>();
        // Список категорий можно хранить отдельно или хардкодить,
        // но здесь для примера возьмем основные.
        // В идеале список категорий лучше передавать из контроллера.
        List<String> categories = List.of("1", "2", "3", "4", "5", "6", "7", "ALL");

        for (String cat : categories) {
            map.put(cat, getLeaderboard(cat));
        }
        return map;
    }

    public List<ScoreEntry> addScore(ScoreEntry newScore) {
        // Валидация
        if (newScore.getName() == null || newScore.getName().trim().isEmpty()) {
            newScore.setName("Anonymous");
        }
        if (newScore.getName().length() > 20) {
            newScore.setName(newScore.getName().substring(0, 20));
        }
        String category = (newScore.getCategory() == null || newScore.getCategory().isEmpty()) ? "ALL" : newScore.getCategory();

        // Проверяем, играл ли уже этот человек в этой категории
        Optional<ScoreEntity> existing = scoreRepository.findByNameAndCategory(newScore.getName(), category);

        if (existing.isPresent()) {
            ScoreEntity old = existing.get();
            // Обновляем только если новый результат лучше
            boolean betterScore = newScore.getScore() > old.getScore();
            boolean sameScoreFaster = (newScore.getScore() == old.getScore()) && (newScore.getTimeSeconds() < old.getTimeSeconds());

            if (betterScore || sameScoreFaster) {
                updateEntity(old, newScore);
                scoreRepository.save(old);
            }
        } else {
            // Создаем новую запись
            ScoreEntity entity = new ScoreEntity();
            entity.setName(newScore.getName());
            entity.setCategory(category);
            updateEntity(entity, newScore);
            scoreRepository.save(entity);
        }

        return getLeaderboard(category);
    }

    private void updateEntity(ScoreEntity target, ScoreEntry source) {
        target.setScore(source.getScore());
        target.setTimeSeconds(source.getTimeSeconds());
        target.setTimeFormatted(source.getTimeFormatted());
        target.setTotalQuestions(source.getTotalQuestions());
    }

    private ScoreEntry convertToDto(ScoreEntity entity) {
        return new ScoreEntry(
                entity.getName(),
                entity.getScore(),
                entity.getTotalQuestions(),
                entity.getTimeFormatted(),
                entity.getTimeSeconds(),
                entity.getCategory()
        );
    }
}