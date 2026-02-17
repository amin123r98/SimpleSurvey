package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Answer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Исправление ошибки:
    // Мы меняем имя колонки в БД на "answer_value", чтобы избежать конфликта со словом "value"
    @Enumerated(EnumType.STRING)
    @Column(name = "answer_value")
    private AnswerValue value;

    @ManyToOne
    private Question question;

    @ManyToOne
    private Submission submission;
}