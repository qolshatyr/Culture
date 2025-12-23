package com.example.quiz.controller;

import com.example.quiz.entity.Question;
import com.example.quiz.repository.QuestionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class QuizController {

    private final QuestionRepository repository;
    private final ObjectMapper objectMapper;

    // Храним рекорды по категориям: Ключ (String) -> Список рекордов
    // "1", "2" ... - это номера групп, "ALL" - это хардкор режим
    private final Map<String, List<ScoreEntry>> leaderboards = new ConcurrentHashMap<>();

    public QuizController(QuestionRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("groups", repository.findDistinctGroups());
        return "index";
    }

    @GetMapping("/study")
    public String studyMode(Model model) {
        model.addAttribute("questions", repository.findAll());
        return "study";
    }

    @GetMapping("/leaderboard")
    public String showLeaderboard(Model model) {
        // Передаем список всех возможных категорий, чтобы нарисовать вкладки, даже если они пустые
        List<String> categories = new ArrayList<>();
        // Добавляем группы
        repository.findDistinctGroups().forEach(g -> categories.add(String.valueOf(g)));
        // Добавляем хардкор
        categories.add("ALL");

        model.addAttribute("categories", categories);
        model.addAttribute("leaderboards", leaderboards);
        return "leaderboard";
    }

    @GetMapping("/quiz/group")
    public String startGroupQuiz(@RequestParam("id") Integer groupId, Model model) throws JsonProcessingException {
        List<Question> questions = repository.findRandomQuestionsByGroup(groupId);
        // Передаем ID категории (например, "1")
        prepareModel(model, questions, "Section " + groupId, String.valueOf(groupId));
        return "quiz";
    }

    @GetMapping("/quiz/all")
    public String startAllQuiz(Model model) throws JsonProcessingException {
        List<Question> questions = new ArrayList<>(repository.findAll());
        Collections.shuffle(questions);
        // Передаем ID категории "ALL"
        prepareModel(model, questions, "HARDCORE MODE", "ALL");
        return "quiz";
    }

    private void prepareModel(Model model, List<Question> questions, String title, String categoryId) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(questions);
        model.addAttribute("questionsJson", json);
        model.addAttribute("currentGroup", title);
        model.addAttribute("categoryId", categoryId); // Важно: сообщаем фронту, какая это категория
    }

    // --- ЛОГИКА ОБНОВЛЕНИЯ РЕКОРДОВ ---
    @PostMapping("/api/submit-score")
    @ResponseBody
    public List<ScoreEntry> submitScore(@RequestBody ScoreEntry newScore) {
        String category = newScore.getCategory();
        if (category == null || category.isEmpty()) {
            category = "ALL";
        }

        // Получаем или создаем список для конкретной категории
        List<ScoreEntry> currentList = leaderboards.computeIfAbsent(category, k -> new ArrayList<>());

        // 1. Ищем, есть ли уже такой игрок в ЭТОЙ категории
        Optional<ScoreEntry> existingEntry = currentList.stream()
                .filter(e -> e.getName().equalsIgnoreCase(newScore.getName()))
                .findFirst();

        if (existingEntry.isPresent()) {
            ScoreEntry oldScore = existingEntry.get();
            // 2. Логика сравнения (Очки > или Очки = но время <)
            boolean betterPoints = newScore.getScore() > oldScore.getScore();
            boolean samePointsButFaster = (newScore.getScore() == oldScore.getScore()) && (newScore.getTimeSeconds() < oldScore.getTimeSeconds());

            if (betterPoints || samePointsButFaster) {
                oldScore.setScore(newScore.getScore());
                oldScore.setTimeSeconds(newScore.getTimeSeconds());
                oldScore.setTimeFormatted(newScore.getTimeFormatted());
                oldScore.setTotalQuestions(newScore.getTotalQuestions());
            }
        } else {
            // 4. Если игрока нет - добавляем
            currentList.add(newScore);
        }

        // 5. Сортируем
        currentList.sort(Comparator.comparingInt(ScoreEntry::getScore).reversed()
                .thenComparingInt(ScoreEntry::getTimeSeconds));

        // 6. Оставляем топ 15
        if (currentList.size() > 15) {
            currentList.subList(15, currentList.size()).clear();
        }

        return currentList;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class ScoreEntry {
        private String name;
        private int score;
        private int totalQuestions;
        private String timeFormatted;
        private int timeSeconds;
        private String category; // Поле для идентификации ("1", "2" или "ALL")
    }
}