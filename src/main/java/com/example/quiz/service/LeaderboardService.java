package com.example.quiz.service;

import com.example.quiz.dto.ScoreEntry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LeaderboardService {

    private final Map<String, List<ScoreEntry>> leaderboards = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Читаем имя файла из настроек application.properties
    @Value("${quiz.data.leaderboard-file:leaderboard_data.json}")
    private String dataFilePath;

    /**
     * Загружаем данные из файла при старте приложения
     */
    @PostConstruct
    public void init() {
        File file = new File(dataFilePath);
        if (file.exists()) {
            try {
                // Читаем JSON обратно в Map
                Map<String, List<ScoreEntry>> data = objectMapper.readValue(
                        file,
                        new TypeReference<Map<String, List<ScoreEntry>>>() {}
                );
                leaderboards.putAll(data);
                System.out.println("✅ Leaderboard data loaded from " + dataFilePath);
            } catch (IOException e) {
                System.err.println("❌ Failed to load leaderboard data: " + e.getMessage());
            }
        }
    }

    public List<ScoreEntry> getLeaderboard(String category) {
        return leaderboards.getOrDefault(category, new ArrayList<>());
    }

    public Map<String, List<ScoreEntry>> getAllLeaderboards() {
        return leaderboards;
    }

    public List<ScoreEntry> addScore(ScoreEntry newScore) {
        // --- 1. ВАЛИДАЦИЯ (Защита от некорректных данных) ---
        if (newScore.getName() == null || newScore.getName().trim().isEmpty()) {
            newScore.setName("Anonymous");
        }
        // Обрезаем имя, если оно слишком длинное (защита верстки)
        if (newScore.getName().length() > 20) {
            newScore.setName(newScore.getName().substring(0, 20));
        }
        // Защита от накрутки (базовая): очки не могут быть отрицательными
        if (newScore.getScore() < 0) {
            newScore.setScore(0);
        }

        String category = newScore.getCategory();
        if (category == null || category.isEmpty()) {
            category = "ALL";
            newScore.setCategory("ALL");
        }

        List<ScoreEntry> currentList = leaderboards.computeIfAbsent(category, k -> Collections.synchronizedList(new ArrayList<>()));

        synchronized (currentList) {
            processScore(currentList, newScore);
        }

        // --- 2. СОХРАНЕНИЕ (Persistence) ---
        saveDataAsync();

        return currentList;
    }

    private void processScore(List<ScoreEntry> list, ScoreEntry newScore) {
        Optional<ScoreEntry> existingEntry = list.stream()
                .filter(e -> e.getName().equalsIgnoreCase(newScore.getName()))
                .findFirst();

        if (existingEntry.isPresent()) {
            ScoreEntry oldScore = existingEntry.get();
            boolean betterPoints = newScore.getScore() > oldScore.getScore();
            boolean samePointsButFaster = (newScore.getScore() == oldScore.getScore()) &&
                    (newScore.getTimeSeconds() < oldScore.getTimeSeconds());

            if (betterPoints || samePointsButFaster) {
                updateEntry(oldScore, newScore);
            }
        } else {
            list.add(newScore);
        }

        list.sort(Comparator.comparingInt(ScoreEntry::getScore).reversed()
                .thenComparingInt(ScoreEntry::getTimeSeconds));

        if (list.size() > 15) {
            list.subList(15, list.size()).clear();
        }
    }

    private void updateEntry(ScoreEntry target, ScoreEntry source) {
        target.setScore(source.getScore());
        target.setTimeSeconds(source.getTimeSeconds());
        target.setTimeFormatted(source.getTimeFormatted());
        target.setTotalQuestions(source.getTotalQuestions());
    }

    /**
     * Сохраняем данные в файл.
     * В реальном Highload-проекте это делали бы асинхронно или по расписанию,
     * но для нашего квиза запись при каждом обновлении допустима.
     */
    private void saveDataAsync() {
        try {
            objectMapper.writeValue(new File(dataFilePath), leaderboards);
        } catch (IOException e) {
            System.err.println("❌ Could not save leaderboard: " + e.getMessage());
            e.printStackTrace();
        }
    }
}