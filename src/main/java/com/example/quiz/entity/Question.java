package com.example.quiz.entity;

import lombok.Data;

@Data
public class Question {
    private Long id;
    private Integer externalId;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String optionE;
    private String topic;
    private Integer groupId;
    private String correctAnswer;
}