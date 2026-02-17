package com.example.demo.repository;

import com.example.demo.model.Submission;
import com.example.demo.model.Survey;
import com.example.demo.model.User; // Не забудь импорт!
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findBySurvey(Survey survey);

    // НОВЫЙ МЕТОД: Проверяет, есть ли запись с таким опросом и юзером
    boolean existsBySurveyAndUser(Survey survey, User user);
}