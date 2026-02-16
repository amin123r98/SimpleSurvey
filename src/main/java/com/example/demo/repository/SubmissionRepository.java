package com.example.demo.repository;

import com.example.demo.model.Submission;
import com.example.demo.model.Survey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findBySurvey(Survey survey);
}