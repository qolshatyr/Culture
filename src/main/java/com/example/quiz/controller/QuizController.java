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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Controller
public class QuizController {

    private final QuestionRepository repository;
    private final ObjectMapper objectMapper;
    private final List<ScoreEntry> leaderboard = new ArrayList<>();

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

    // НОВОЕ: Отдельная страница рейтинга
    @GetMapping("/leaderboard")
    public String showLeaderboard(Model model) {
        model.addAttribute("scores", leaderboard);
        return "leaderboard"; // Нужен файл leaderboard.html
    }

    @GetMapping("/quiz/group")
    public String startGroupQuiz(@RequestParam("id") Integer groupId, Model model) throws JsonProcessingException {
        List<Question> questions = repository.findRandomQuestionsByGroup(groupId);
        prepareModel(model, questions, "Section " + groupId);
        return "quiz";
    }

    @GetMapping("/quiz/all")
    public String startAllQuiz(Model model) throws JsonProcessingException {
        List<Question> questions = new ArrayList<>(repository.findAll());
        Collections.shuffle(questions);
        prepareModel(model, questions, "ALL SECTIONS");
        return "quiz";
    }

    private void prepareModel(Model model, List<Question> questions, String title) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(questions);
        model.addAttribute("questionsJson", json);
        model.addAttribute("currentGroup", title);
    }

    // --- ЛОГИКА ОБНОВЛЕНИЯ РЕКОРДОВ ---
    @PostMapping("/api/submit-score")
    @ResponseBody
    public List<ScoreEntry> submitScore(@RequestBody ScoreEntry newScore) {
        // 1. Ищем, есть ли уже такой игрок (без учета регистра: User = user)
        Optional<ScoreEntry> existingEntry = leaderboard.stream()
                .filter(e -> e.getName().equalsIgnoreCase(newScore.getName()))
                .findFirst();

        if (existingEntry.isPresent()) {
            ScoreEntry oldScore = existingEntry.get();

            // 2. Логика сравнения:
            boolean betterPoints = newScore.getScore() > oldScore.getScore();
            boolean samePointsButFaster = (newScore.getScore() == oldScore.getScore()) && (newScore.getTimeSeconds() < oldScore.getTimeSeconds());

            // 3. Если новый результат лучше - обновляем
            if (betterPoints || samePointsButFaster) {
                oldScore.setScore(newScore.getScore());
                oldScore.setTimeSeconds(newScore.getTimeSeconds());
                oldScore.setTimeFormatted(newScore.getTimeFormatted());
                oldScore.setTotalQuestions(newScore.getTotalQuestions());
                // Можно добавить флаг "updated", чтобы на фронте показать "New Record!"
            }
        } else {
            // 4. Если игрока нет - добавляем
            leaderboard.add(newScore);
        }

        // 5. Сортируем: Сначала по очкам (убывание), потом по времени (возрастание)
        leaderboard.sort(Comparator.comparingInt(ScoreEntry::getScore).reversed()
                .thenComparingInt(ScoreEntry::getTimeSeconds));

        // 6. Оставляем топ 15 (увеличим немного лимит)
        if (leaderboard.size() > 15) {
            leaderboard.subList(15, leaderboard.size()).clear();
        }

        return leaderboard;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class ScoreEntry {
        private String name;
        private int score;
        private int totalQuestions;
        private String timeFormatted;
        private int timeSeconds;
    }
}