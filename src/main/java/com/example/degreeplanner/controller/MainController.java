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

        boolean englishDone = false;
        boolean mathModelingDone = false;
        int ahCompleted = 0;
        int ahRequired = 2;

        int shCompleted = 0;
        int shRequired = 2;
        int nmCompleted = 0;
        int nmRequired = 2;

        boolean worldCulturesDone = false;
        int csCoreCompleted = 0;
        int csCoreTotal = 5;

        int mathReqCompleted = 0;
        int mathReqTotal = 3;
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
                }
            }
            if ("FOLK-F 101".equalsIgnoreCase(code)
                    || "FOLK-F 131".equalsIgnoreCase(code)
                    || "MSCH-C 101".equalsIgnoreCase(code)) {
                ahCompleted++;
            }

            if ("INTL-I 100".equalsIgnoreCase(code)
                    || "BUS-A 200".equalsIgnoreCase(code)
                    || "HIST-J 300".equalsIgnoreCase(code)) {
                shCompleted++;
            }
            if ("PHYS-P 120".equalsIgnoreCase(code)
                    || "AST-A 100".equalsIgnoreCase(code)) {
                nmCompleted++;
            }
            if ("MELC-M 204".equalsIgnoreCase(code)) {
                worldCulturesDone = true;
            }
            if ("CSCI-C 211".equalsIgnoreCase(code)
                    || "CSCI-C 212".equalsIgnoreCase(code)
                    || "CSCI-C 241".equalsIgnoreCase(code)
                    || "CSCI-C 343".equalsIgnoreCase(code)
                    || "CSCI-Y 395".equalsIgnoreCase(code)) {
                csCoreCompleted++;
            }
        }
        int genEdPiecesCompleted = 0;
        int genEdPiecesTotal = 0;

        genEdPiecesCompleted += englishDone ? 1 : 0;
        genEdPiecesTotal += 1;

        genEdPiecesCompleted += mathModelingDone ? 1 : 0;
        genEdPiecesTotal += 1;

        genEdPiecesCompleted += Math.min(ahCompleted, ahRequired);
        genEdPiecesTotal += ahRequired;

        genEdPiecesCompleted += Math.min(shCompleted, shRequired);
        genEdPiecesTotal += shRequired;

        genEdPiecesCompleted += Math.min(nmCompleted, nmRequired);
        genEdPiecesTotal += nmRequired;

        genEdPiecesCompleted += worldCulturesDone ? 1 : 0;
        genEdPiecesTotal += 1;

        int csProgress = csCoreTotal == 0 ? 0 : (int) Math.round((csCoreCompleted * 100.0) / csCoreTotal);
        int genEdProgress = genEdPiecesTotal == 0 ? 0 : (int) Math.round((genEdPiecesCompleted * 100.0) / genEdPiecesTotal);

        int totalPiecesCompleted = genEdPiecesCompleted + csCoreCompleted + mathReqCompleted;
        int totalPiecesTotal = genEdPiecesTotal + csCoreTotal + mathReqTotal;
        int totalProgress = totalPiecesTotal == 0 ? 0 : (int) Math.round((totalPiecesCompleted * 100.0) / totalPiecesTotal);

        model.addAttribute("student", student);
        model.addAttribute("currentTerm", student.getCurrentSemester());
        model.addAttribute("currentTermCourses", currentTermCourses);
        model.addAttribute("csProgress", csProgress);
        model.addAttribute("genEdProgress", genEdProgress);
        model.addAttribute("totalProgress", totalProgress);

        return "dashboard";
    }

    @GetMapping("/plan")
    public String planSemester(@RequestParam(value = "term", required = false) String termOverride,
                               HttpSession session,
                               Model model) {
        Student student = (Student) session.getAttribute("loggedInStudent");
        if (student == null) {
            return "redirect:/";
        }

        List<Course> allCourses = courseRepo.findByStudentId(student.getId());

        String planningTerm;
        if (termOverride != null && !termOverride.isBlank()) {
            planningTerm = termOverride;
        } else {
            planningTerm = determineNextPlanningTerm(student, allCourses);
        }

        //courses already in the active planning sem (no final grades yet for that sem)
        List<Course> planningTermCourses = new ArrayList<>();
        for (Course c : allCourses) {
            if (planningTerm != null && planningTerm.equals(c.getTerm())) {
                planningTermCourses.add(c);
            }
        }

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
        int mathReqPlanned = 0;
        int mathReqTotal = 3;
        List<Course> mathReqCourses = new ArrayList<>();
        for (Course c : allCourses) {
            String code = c.getCourseCode();
            String grade = c.getGrade();

            boolean isMathReqCourse = "MATH-M 211".equalsIgnoreCase(code)
                    || "MATH-M 301".equalsIgnoreCase(code)
                    || "MATH-M 365".equalsIgnoreCase(code);

            if (!isPassingGrade(grade)) {
                if (isMathReqCourse && grade != null && grade.trim().equalsIgnoreCase("NR")) {
                    if (mathReqCompleted + mathReqPlanned < mathReqTotal) {
                        mathReqPlanned++;
                    }
                }
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
        Map<String, Boolean> termEditable = new LinkedHashMap<>();
        for (Course c : allCourses) {
            String term = c.getTerm();
            if (term == null || term.isBlank()) {
                term = "Other";
            }
            if (!coursesByTerm.containsKey(term)) {
                coursesByTerm.put(term, new ArrayList<>());
            }
            coursesByTerm.get(term).add(c);

            // A term is considered editable if it has any course without a final letter grade (e.g., NR or null)
            boolean notFinal = (c.getGrade() == null) || "NR".equalsIgnoreCase(c.getGrade().trim());
            termEditable.put(term, termEditable.getOrDefault(term, Boolean.FALSE) || notFinal);
        }

        List<Course> nextSemesterCatalog = buildNextSemesterCatalog(allCourses);

        model.addAttribute("student", student);
        model.addAttribute("coursesByTerm", coursesByTerm);
        model.addAttribute("termEditable", termEditable);
        model.addAttribute("nextSemesterCatalog", nextSemesterCatalog);
        model.addAttribute("planningTerm", planningTerm);
        model.addAttribute("planningTermCourses", planningTermCourses);

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
        model.addAttribute("mathReqPlanned", mathReqPlanned);
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

    private List<Course> buildNextSemesterCatalog(List<Course> allCourses) {
        //Base catalog
        List<Course> baseCatalog = new ArrayList<>();
        //CSmajor
        baseCatalog.add(buildDummy("CSCI-B 403", "Introduction to Algorithm Design and Analysis", "3"));
        baseCatalog.add(buildDummy("CSCI-B 438", "Fundamentals of Computer Networks", "3"));
        baseCatalog.add(buildDummy("CSCI-B 443", "Systems Programming", "3"));
        baseCatalog.add(buildDummy("CSCI-B 465", "Data Mining", "3"));
        baseCatalog.add(buildDummy("CSCI-B 490", "Topics in Computer Science", "3"));
        baseCatalog.add(buildDummy("CSCI-C 4xx", "Advanced Topics in CS", "3"));
        baseCatalog.add(buildDummy("CSCI-P 424", "Advanced Programming Concepts", "3"));
        //Math stats
        baseCatalog.add(buildDummy("MATH-M 301", "Linear Algebra and Applications", "3"));
        baseCatalog.add(buildDummy("MATH-M 365", "Introduction to Probability and Statistics", "3"));
        //electives
        baseCatalog.add(buildDummy("INFO-I 421", "Applications of Data Mining", "3"));
        baseCatalog.add(buildDummy("BUS-M 300", "Introduction to Marketing", "3"));
        baseCatalog.add(buildDummy("PHIL-P 100", "Introduction to Philosophy", "3"));
        baseCatalog.add(buildDummy("PSY-P 101", "Introductory Psychology I", "3"));
        baseCatalog.add(buildDummy("AST-A 110", "Introduction to Astronomy", "3"));
        List<String> existingCodes = new ArrayList<>();
        for (Course c : allCourses) {
            if (c.getCourseCode() != null) {
                existingCodes.add(c.getCourseCode().toUpperCase());
            }
        }

        List<Course> filtered = new ArrayList<>();
        for (Course candidate : baseCatalog) {
            String code = candidate.getCourseCode();
            if (code == null) continue;
            if (!existingCodes.contains(code.toUpperCase())) {
                filtered.add(candidate);
            }
        }
        return filtered;
    }
    private String determineNextPlanningTerm(Student student, List<Course> allCourses) {
        String latestTerm = null;

        if (student.getCurrentSemester() != null && !student.getCurrentSemester().isBlank()) {
            latestTerm = student.getCurrentSemester();
        }

        for (Course c : allCourses) {
            String term = c.getTerm();
            if (term == null || term.isBlank()) {
                continue;
            }
            if (latestTerm == null || compareTerms(term, latestTerm) > 0) {
                latestTerm = term;
            }
        }

        if (latestTerm == null) {
            return "Fall 2026";
        }

        return nextTerm(latestTerm);
    }
    private int compareTerms(String a, String b) {
        Term ta = parseTerm(a);
        Term tb = parseTerm(b);
        if (ta == null && tb == null) return 0;
        if (ta == null) return -1;
        if (tb == null) return 1;
        if (ta.year != tb.year) {
            return Integer.compare(ta.year, tb.year);
        }
        return Integer.compare(ta.seasonOrder, tb.seasonOrder);
    }
    private String nextTerm(String current) {
        Term t = parseTerm(current);
        if (t == null) {
            return "Fall 2026";
        }
        
        if (t.seasonOrder == 1) { 
            return "Fall " + t.year;
        } else { 
            return "Spring " + (t.year + 1);
        }
    }
    private Term parseTerm(String term) {
        if (term == null) return null;
        String trimmed = term.trim();
        if (trimmed.isEmpty()) return null;
        String[] parts = trimmed.split("\\s+");
        if (parts.length != 2) return null;
        String season = parts[0];
        int year;
        try {
            year = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
        int order;
        if (season.equalsIgnoreCase("Spring")) {
            order = 1;
        } else if (season.equalsIgnoreCase("Fall")) {
            order = 2;
        } else {
            return null;
        }
        return new Term(year, order);
    }

    private static class Term {
        final int year;
        final int seasonOrder;
        Term(int year, int seasonOrder) {
            this.year = year;
            this.seasonOrder = seasonOrder;
        }
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

    @PostMapping("/plan")
    public String savePlannedSemester(@RequestParam(value = "selectedCourses", required = false) List<String> selectedCourseCodes,
                                      @RequestParam(value = "removeCourseIds", required = false) List<Long> removeCourseIds,
                                      @RequestParam(value = "term", required = false) String termOverride,
                                      HttpSession session) {
        Student student = (Student) session.getAttribute("loggedInStudent");
        if (student == null) {
            return "redirect:/";
        }

        List<Course> allCourses = courseRepo.findByStudentId(student.getId());
        String planningTerm;
        if (termOverride != null && !termOverride.isBlank()) {
            planningTerm = termOverride;
        } else {
            planningTerm = determineNextPlanningTerm(student, allCourses);
        }
        if (selectedCourseCodes != null) {
            for (String code : selectedCourseCodes) {
                if (code == null || code.isBlank()) continue;
                Course planned = new Course();
                planned.setStudentId(student.getId());
                planned.setCourseCode(code);
                if (code.equalsIgnoreCase("CSCI-C 4xx")) {
                    planned.setTitle("Advanced Topics in CS (3 cr)");
                } else {
                    planned.setTitle(code);
                }

                planned.setGrade("NR");
                planned.setTerm(planningTerm);
                courseRepo.save(planned);
            }
        }

        if (removeCourseIds != null) {
            for (Long courseId : removeCourseIds) {
                if (courseId == null) continue;
                courseRepo.deleteById(courseId);
            }
        }

        return "redirect:/plan";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
