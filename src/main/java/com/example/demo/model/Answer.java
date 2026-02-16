package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Answer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Boolean yesNoValue;

    @ManyToOne
    private Question question;

    @ManyToOne
    private Submission submission;
}