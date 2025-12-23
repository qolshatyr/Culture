package com.example.quiz.controller;

import com.example.quiz.dto.ScoreEntry;
import com.example.quiz.entity.Question;
import com.example.quiz.repository.QuestionRepository;
import com.example.quiz.service.LeaderboardService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor // Lombok создаст конструктор для final полей
public class QuizController {

    private final QuestionRepository repository;
    private final LeaderboardService leaderboardService; // Подключаем сервис
    private final ObjectMapper objectMapper = new ObjectMapper();

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
    public String showLeaderboard(@RequestParam(value = "highlight", required = false) String highlightName, Model model) {
        List<String> categories = new ArrayList<>();
        repository.findDistinctGroups().forEach(g -> categories.add(String.valueOf(g)));
        categories.add("ALL");

        model.addAttribute("categories", categories);
        model.addAttribute("leaderboards", leaderboardService.getAllLeaderboards());

        // Передаем имя для подсветки, если оно есть
        if (highlightName != null && !highlightName.isEmpty()) {
            model.addAttribute("highlight", highlightName);
        }

        return "leaderboard";
    }

    @GetMapping("/quiz/group")
    public String startGroupQuiz(@RequestParam("id") Integer groupId, Model model) throws JsonProcessingException {
        List<Question> questions = repository.findRandomQuestionsByGroup(groupId);
        prepareModel(model, questions, "Section " + groupId, String.valueOf(groupId));
        return "quiz";
    }

    @GetMapping("/quiz/all")
    public String startAllQuiz(Model model) throws JsonProcessingException {
        List<Question> questions = new ArrayList<>(repository.findAll());
        Collections.shuffle(questions);
        prepareModel(model, questions, "HARDCORE MODE", "ALL");
        return "quiz";
    }

    private void prepareModel(Model model, List<Question> questions, String title, String categoryId) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(questions);
        model.addAttribute("questionsJson", json);
        model.addAttribute("currentGroup", title);
        model.addAttribute("categoryId", categoryId);
    }

    // --- API ---
    @PostMapping("/api/submit-score")
    @ResponseBody
    public List<ScoreEntry> submitScore(@RequestBody ScoreEntry newScore) {
        // Вся логика теперь внутри одной строчки
        return leaderboardService.addScore(newScore);
    }
}