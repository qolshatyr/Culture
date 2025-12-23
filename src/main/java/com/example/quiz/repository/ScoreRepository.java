package com.example.quiz.repository;

import com.example.quiz.entity.ScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ScoreRepository extends JpaRepository<ScoreEntity, Long> {
    // Найти записи по категории, отсортированные по очкам (убывание) и времени (возрастание)
    List<ScoreEntity> findByCategoryOrderByScoreDescTimeSecondsAsc(String category);

    // Найти запись конкретного игрока в конкретной категории
    Optional<ScoreEntity> findByNameAndCategory(String name, String category);
}