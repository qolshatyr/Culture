package com.example.quiz.repository;

import com.example.quiz.entity.Question;
import jakarta.annotation.PostConstruct;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuestionRepository {

    private final List<Question> allQuestions = new ArrayList<>();

    @PostConstruct
    public void init() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("questions.csv")) {
            if (is == null) {
                System.err.println("❌ Файл questions.csv не найден в resources!");
                return;
            }

            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

                long idCounter = 1;
                for (CSVRecord rec : csvParser) {
                    try {
                        Question q = new Question();
                        q.setId(idCounter++);
                        q.setExternalId(parseInt(rec.get("№")));
                        q.setQuestionText(rec.get("Вопрос"));
                        q.setOptionA(rec.get("Вариант A"));
                        q.setOptionB(rec.get("Вариант B"));
                        q.setOptionC(rec.get("Вариант C"));
                        q.setOptionD(rec.get("Вариант D"));
                        q.setOptionE(rec.get("Вариант E"));
                        q.setTopic(rec.get("Topic"));
                        q.setGroupId(parseInt(rec.get("Group_ID")));
                        q.setCorrectAnswer(rec.get("Correct_Answer"));
                        allQuestions.add(q);
                    } catch (Exception e) {
                        // Пропускаем битые строки
                    }
                }
                System.out.println("✅ Успешно загружено вопросов: " + allQuestions.size());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Integer parseInt(String value) {
        try { return Integer.parseInt(value); } catch (Exception e) { return 0; }
    }

    public List<Question> findAll() { return new ArrayList<>(allQuestions); }

    public List<Integer> findDistinctGroups() {
        return allQuestions.stream().map(Question::getGroupId).distinct().sorted().collect(Collectors.toList());
    }

    public List<Question> findRandomQuestionsByGroup(Integer groupId) {
        List<Question> filtered = allQuestions.stream()
                .filter(q -> q.getGroupId().equals(groupId))
                .collect(Collectors.toList());
        Collections.shuffle(filtered);
        return filtered.stream().limit(20).collect(Collectors.toList());
    }
}