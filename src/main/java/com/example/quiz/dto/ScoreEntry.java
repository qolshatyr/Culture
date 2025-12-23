package com.example.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScoreEntry {
    private String name;
    private int score;
    private int totalQuestions;
    private String timeFormatted;
    private int timeSeconds;
    private String category; // "1", "2" или "ALL"
}