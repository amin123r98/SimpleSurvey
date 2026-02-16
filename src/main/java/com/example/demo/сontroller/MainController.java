package com.example.demo.controller; // проверь, чтобы имя пакета было верным

import com.example.demo.model.Survey;
import com.example.demo.model.Question;
import com.example.demo.model.User;
import com.example.demo.model.Submission;
import com.example.demo.model.Answer;
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
        // Ограничение 10 вопросов
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
            return "redirect:/login"; // или регистрация
        }

        Survey survey = surveyRepo.findByUuid(uuid);
        model.addAttribute("survey", survey);
        return "take_survey";
    }

    @PostMapping("/survey/{uuid}")
    public String submitSurvey(@PathVariable String uuid, @RequestParam Map<String, String> params, HttpSession session) {
        User user = (User) session.getAttribute("user");
        Survey survey = surveyRepo.findByUuid(uuid);

        Submission submission = new Submission();
        submission.setSurvey(survey);
        submission.setUser(user);
        submissionRepo.save(submission);

        for (Question q : survey.getQuestions()) {
            String val = params.get("q_" + q.getId()); // получаем "true" или "false"
            Answer a = new Answer();
            a.setQuestion(q);
            a.setYesNoValue(Boolean.valueOf(val));
            a.setSubmission(submission);
            answerRepo.save(a);
        }

        return "redirect:/home";
    }

    // --- МЕНЕДЖЕР (Статистика) ---
    @GetMapping("/manager")
    public String managerPage() { return "manager_search"; }

    @PostMapping("/manager/stats")
    public String getStats(@RequestParam String linkOrUuid, Model model) {
        // 1. Убираем лишние пробелы
        String cleanLink = linkOrUuid.trim();

        // 2. Логика извлечения UUID (если в конце есть слэш - убираем его)
        if (cleanLink.endsWith("/")) {
            cleanLink = cleanLink.substring(0, cleanLink.length() - 1);
        }

        String uuid;
        if (cleanLink.contains("/")) {
            // Если это полная ссылка http://.../survey/123-abc
            uuid = cleanLink.substring(cleanLink.lastIndexOf("/") + 1);
        } else {
            // Если вставили только код
            uuid = cleanLink;
        }

        // ВЫВОД В КОНСОЛЬ (Смотри сюда при запуске!)
        System.out.println("Менеджер ищет UUID: " + uuid);

        Survey survey = surveyRepo.findByUuid(uuid);

        if (survey == null) {
            System.out.println("Опрос с таким UUID не найден в БД!");
            model.addAttribute("error", "Опрос не найден! Возможно, вы перезапустили сервер (БД очистилась) или ссылка неверна.");
            return "manager_search"; // Возвращаем на страницу поиска с ошибкой
        }

        List<Submission> submissions = submissionRepo.findBySurvey(survey);
        System.out.println("Найдено прохождений: " + submissions.size());

        model.addAttribute("count", submissions.size()); // Добавляем количество
        model.addAttribute("submissions", submissions);
        return "manager_stats";
    }
}