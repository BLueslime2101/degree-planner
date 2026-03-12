package com.example.degreeplanner.controller;

import com.example.degreeplanner.domain.Course;
import com.example.degreeplanner.domain.Student;
import com.example.degreeplanner.repository.CourseRepository;
import com.example.degreeplanner.repository.StudentRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class MainController {

    private final StudentRepository studentRepo;
    private final CourseRepository courseRepo;

    public MainController(StudentRepository studentRepo, CourseRepository courseRepo) {
        this.studentRepo = studentRepo;
        this.courseRepo = courseRepo;
    }

    @GetMapping("/")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam("studentUsername") String studentUsername,
                          @RequestParam("password") String password,
                          HttpSession session,
                          Model model) {
        // Hard-coded demo passwords for the two sample accounts
        boolean passwordOk =
                ("student1".equalsIgnoreCase(studentUsername) && "password".equals(password)) ||
                ("shisara".equalsIgnoreCase(studentUsername) && "shisara123".equals(password));

        if (!passwordOk) {
            model.addAttribute("loginError", "Invalid username or password for demo account.");
            return "login";
        }

        Optional<Student> optStudent = studentRepo.findFirstByStudentUsername(studentUsername);
        if (optStudent.isPresent()) {
            Student student = optStudent.get();
            session.setAttribute("loggedInStudent", student);
            return "redirect:/dashboard";
        }

        model.addAttribute("loginError", "No matching demo student found.");
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Student student = (Student) session.getAttribute("loggedInStudent");
        if (student == null)
            return "redirect:/";

        List<Course> allCourses = courseRepo.findByStudentId(student.getId());

        List<Course> currentTermCourses = new ArrayList<>();
        for (Course c : allCourses) {
            if (c.getGrade() != null && c.getGrade().equalsIgnoreCase("NR")) {
                currentTermCourses.add(c);
            }
        }

        int totalCourses = allCourses.size();
        int csProgress = Math.min((totalCourses * 15), 100);
        int genEdProgress = Math.min((totalCourses * 25), 100);
        int totalProgress = Math.min((totalCourses * 10), 100);

        model.addAttribute("student", student);
        model.addAttribute("currentTerm", student.getCurrentSemester());
        model.addAttribute("currentTermCourses", currentTermCourses);
        model.addAttribute("csProgress", csProgress);
        model.addAttribute("genEdProgress", genEdProgress);
        model.addAttribute("totalProgress", totalProgress);

        return "dashboard";
    }

    @GetMapping("/plan")
    public String planSemester(HttpSession session, Model model) {
        Student student = (Student) session.getAttribute("loggedInStudent");
        if (student == null) {
            return "redirect:/";
        }

        List<Course> allCourses = courseRepo.findByStudentId(student.getId());

        boolean englishDone = false;
        boolean mathModelingDone = false;

        int ahCompleted = 0;
        int ahRequired = 2;
        List<Course> ahCourses = new ArrayList<>();

        int shCompleted = 0;
        int shRequired = 2;
        List<Course> shCourses = new ArrayList<>();

        int nmCompleted = 0;
        int nmRequired = 2;
        List<Course> nmCourses = new ArrayList<>();

        boolean worldCulturesDone = false;
        List<Course> worldCulturesCourses = new ArrayList<>();

        int csCoreCompleted = 0;
        int csCoreTotal = 5;
        List<Course> csCoreCourses = new ArrayList<>();

        int mathReqCompleted = 0;
        int mathReqTotal = 3;
        List<Course> mathReqCourses = new ArrayList<>();

        for (Course c : allCourses) {
            String code = c.getCourseCode();

            if (!isPassingGrade(c.getGrade())) {
                continue;
            }

            if ("ENG-W 131".equalsIgnoreCase(code)) {
                englishDone = true;
            }

            if ("MATH-M 211".equalsIgnoreCase(code)) {
                mathModelingDone = true;
                if (mathReqCompleted == 0) {
                    mathReqCompleted = 1;
                    mathReqCourses.add(c);
                }
            }

            if ("FOLK-F 101".equalsIgnoreCase(code)
                    || "FOLK-F 131".equalsIgnoreCase(code)
                    || "MSCH-C 101".equalsIgnoreCase(code)) {
                ahCompleted++;
                ahCourses.add(c);
            }

            if ("INTL-I 100".equalsIgnoreCase(code)
                    || "BUS-A 200".equalsIgnoreCase(code)
                    || "HIST-J 300".equalsIgnoreCase(code)) {
                shCompleted++;
                shCourses.add(c);
            }

            if ("PHYS-P 120".equalsIgnoreCase(code)
                    || "AST-A 100".equalsIgnoreCase(code)) {
                nmCompleted++;
                nmCourses.add(c);
            }

            if ("MELC-M 204".equalsIgnoreCase(code)) {
                worldCulturesDone = true;
                worldCulturesCourses.add(c);
            }

            if ("CSCI-C 211".equalsIgnoreCase(code)
                    || "CSCI-C 212".equalsIgnoreCase(code)
                    || "CSCI-C 241".equalsIgnoreCase(code)
                    || "CSCI-C 343".equalsIgnoreCase(code)
                    || "CSCI-Y 395".equalsIgnoreCase(code)) {
                csCoreCompleted++;
                csCoreCourses.add(c);
            }
        }

        Map<String, List<Course>> coursesByTerm = new LinkedHashMap<>();
        for (Course c : allCourses) {
            String term = c.getTerm();
            if (term == null || term.isBlank()) {
                term = "Other";
            }
            if (!coursesByTerm.containsKey(term)) {
                coursesByTerm.put(term, new ArrayList<>());
            }
            coursesByTerm.get(term).add(c);
        }

        List<Course> nextSemesterCatalog = new ArrayList<>();
        nextSemesterCatalog.add(buildDummy("CSCI-C 4xx", "Advanced Topics in CS", "3"));
        

        model.addAttribute("student", student);
        model.addAttribute("coursesByTerm", coursesByTerm);
        model.addAttribute("nextSemesterCatalog", nextSemesterCatalog);

        model.addAttribute("englishDone", englishDone);
        model.addAttribute("mathModelingDone", mathModelingDone);
        model.addAttribute("ahCompleted", ahCompleted);
        model.addAttribute("ahRequired", ahRequired);
        model.addAttribute("shCompleted", shCompleted);
        model.addAttribute("shRequired", shRequired);
        model.addAttribute("nmCompleted", nmCompleted);
        model.addAttribute("nmRequired", nmRequired);
        model.addAttribute("worldCulturesDone", worldCulturesDone);
        model.addAttribute("csCoreCompleted", csCoreCompleted);
        model.addAttribute("csCoreTotal", csCoreTotal);
        model.addAttribute("mathReqCompleted", mathReqCompleted);
        model.addAttribute("mathReqTotal", mathReqTotal);

        model.addAttribute("ahCourses", ahCourses);
        model.addAttribute("shCourses", shCourses);
        model.addAttribute("nmCourses", nmCourses);
        model.addAttribute("worldCulturesCourses", worldCulturesCourses);
        model.addAttribute("csCoreCourses", csCoreCourses);
        model.addAttribute("mathReqCourses", mathReqCourses);

        return "plan";
    }

    private Course buildDummy(String code, String title, String creditsLabel) {
        Course c = new Course();
        c.setCourseCode(code);
        c.setTitle(title + " (" + creditsLabel + " cr)");
        c.setGrade(null);
        return c;
    }

    private boolean isPassingGrade(String grade) {
        if (grade == null)
            return false;
        String g = grade.trim().toUpperCase();
        if (g.isEmpty())
            return false;
        if (g.equals("W") || g.equals("NR"))
            return false;
        return true;
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
