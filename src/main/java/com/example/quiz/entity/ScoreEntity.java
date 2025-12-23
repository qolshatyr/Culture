package com.example.quiz.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "leaderboard")
@Data
public class ScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int score;
    private int totalQuestions;
    private String timeFormatted;
    private int timeSeconds;
    private String category;
}