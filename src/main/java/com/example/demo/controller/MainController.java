package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class MainController {
    @Autowired private SurveyRepository surveyRepo;
    @Autowired private QuestionRepository questionRepo;
    @Autowired private SubmissionRepository submissionRepo;
    @Autowired private AnswerRepository answerRepo;

    // --- ДОМОЙ ---
    @GetMapping("/home")
    public String home(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        return "home";
    }

    // --- АДМИН (Создание) ---
    @GetMapping("/admin/create")
    public String createSurveyForm(Model model) {
        model.addAttribute("survey", new Survey());
        return "create_survey";
    }

    @PostMapping("/admin/create")
    public String createSurvey(@RequestParam List<String> questionTexts, Model model) {
        if (questionTexts.size() > 10) return "error";

        Survey survey = new Survey();
        survey.setUuid(UUID.randomUUID().toString());

        for (String text : questionTexts) {
            if (!text.isEmpty()) {
                Question q = new Question();
                q.setText(text);
                q.setSurvey(survey);
                survey.getQuestions().add(q);
            }
        }
        surveyRepo.save(survey);

        String link = "http://localhost:8080/survey/" + survey.getUuid();
        model.addAttribute("link", link);
        return "survey_created";
    }

    // --- ЮЗЕР (Прохождение) ---
    @GetMapping("/survey/{uuid}")
    public String takeSurvey(@PathVariable String uuid, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            session.setAttribute("redirectAfterLogin", "/survey/" + uuid);
            return "redirect:/login";
        }

        Survey survey = surveyRepo.findByUuid(uuid);
        if (survey == null) {
            model.addAttribute("error", "Опрос не найден");
            return "user_search";
        }

        // Проверка: проходил ли уже?
        if (submissionRepo.existsBySurveyAndUser(survey, user)) {
            return "survey_passed";
        }

        model.addAttribute("survey", survey);
        return "take_survey";
    }

    @PostMapping("/survey/{uuid}")
    public String submitSurvey(@PathVariable String uuid, @RequestParam Map<String, String> params, HttpSession session) {
        User user = (User) session.getAttribute("user");
        Survey survey = surveyRepo.findByUuid(uuid);

        if (submissionRepo.existsBySurveyAndUser(survey, user)) {
            return "survey_passed";
        }

        Submission submission = new Submission();
        submission.setSurvey(survey);
        submission.setUser(user);
        submissionRepo.save(submission);

        for (Question q : survey.getQuestions()) {
            String val = params.get("q_" + q.getId());
            if (val != null) {
                Answer a = new Answer();
                a.setQuestion(q);
                a.setValue(AnswerValue.valueOf(val));
                a.setSubmission(submission);
                answerRepo.save(a);
            }
        }

        return "redirect:/home";
    }

    // --- МЕНЕДЖЕР (Статистика) ---
    @GetMapping("/manager")
    public String managerPage() { return "manager_search"; }

    @PostMapping("/manager/stats")
    public String getStats(@RequestParam String linkOrUuid, Model model) {
        String cleanLink = linkOrUuid.trim();
        if (cleanLink.endsWith("/")) cleanLink = cleanLink.substring(0, cleanLink.length() - 1);
        String uuid = cleanLink.contains("/") ? cleanLink.substring(cleanLink.lastIndexOf("/") + 1) : cleanLink;

        Survey survey = surveyRepo.findByUuid(uuid);
        if (survey == null) {
            model.addAttribute("error", "Опрос не найден!");
            return "manager_search";
        }

        List<Submission> submissions = submissionRepo.findBySurvey(survey);

        // --- СБОР АНАЛИТИКИ ПО ВОПРОСАМ ---
        // Карта: ID вопроса -> { "YES": кол-во, "NO": кол-во, "NOT_SURE": кол-во }
        Map<Long, Map<String, Integer>> stats = new HashMap<>();

        for (Question q : survey.getQuestions()) {
            Map<String, Integer> counts = new HashMap<>();
            counts.put("YES", 0);
            counts.put("NO", 0);
            counts.put("NOT_SURE", 0);

            for (Submission sub : submissions) {
                for (Answer ans : sub.getAnswers()) {
                    if (ans.getQuestion().getId().equals(q.getId())) {
                        String val = ans.getValue().name();
                        counts.put(val, counts.get(val) + 1);
                    }
                }
            }
            stats.put(q.getId(), counts);
        }

        model.addAttribute("survey", survey);
        model.addAttribute("count", submissions.size());
        model.addAttribute("submissions", submissions);
        model.addAttribute("stats", stats); // Отправляем расчеты в HTML
        return "manager_stats";
    }

    // --- ЮЗЕР (Поиск опроса) ---
    @GetMapping("/user/search")
    public String userSearchPage() {
        return "user_search";
    }

    @PostMapping("/user/search")
    public String searchSurveyToTake(@RequestParam String linkOrUuid, Model model) {
        String cleanLink = linkOrUuid.trim();
        if (cleanLink.endsWith("/")) cleanLink = cleanLink.substring(0, cleanLink.length() - 1);
        String uuid = cleanLink.contains("/") ? cleanLink.substring(cleanLink.lastIndexOf("/") + 1) : cleanLink;

        Survey survey = surveyRepo.findByUuid(uuid);
        if (survey == null) {
            model.addAttribute("error", "Опрос не найден!");
            return "user_search";
        }

        return "redirect:/survey/" + uuid;
    }
}