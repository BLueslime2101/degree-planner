package com.example.degreeplanner;

import com.example.degreeplanner.domain.Course;
import com.example.degreeplanner.domain.Student;
import com.example.degreeplanner.repository.CourseRepository;
import com.example.degreeplanner.repository.StudentRepository;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final StudentRepository studentRepo;
    private final CourseRepository courseRepo;

    public DataLoader(StudentRepository studentRepo, CourseRepository courseRepo) {
        this.studentRepo = studentRepo;
        this.courseRepo = courseRepo;
    }

    @Override
    public void run(String... args) throws Exception {
        
        String demoUsername = "student1";

        List<Student> matches = studentRepo.findAllByStudentUsername(demoUsername);
        if (matches.size() > 1) {
            Student keeper = matches.get(0);
            for (int i = 1; i < matches.size(); i++) {
                studentRepo.delete(matches.get(i));
            }
            matches = List.of(keeper);
        }

        Student s1;
        if (matches.isEmpty()) {
            s1 = new Student();
            s1.setStudentUsername(demoUsername);
            s1.setCurrentSemester("Spring 2026");
            s1 = studentRepo.save(s1);
        } else {
            s1 = matches.get(0);
            s1.setCurrentSemester("Spring 2026");
            s1 = studentRepo.save(s1);
        }

        // avoid duplicating course data if it already exists
        if (!courseRepo.findByStudentId(s1.getId()).isEmpty()) {
            return;
        }

        //Fall 2023
        addCourse(s1, "CSCI-C 211", "Intro to Computer Science", "B+", "Fall 2023");
        addCourse(s1, "ENG-W 131", "Reading, Writing, & Inquiry I", "B", "Fall 2023");
        addCourse(s1, "ENGR-E 101", "Innovation and Design", "A-", "Fall 2023");
        addCourse(s1, "LLLC-Y 101", "Tech Leadership & Innovation I", "A+", "Fall 2023");
        addCourse(s1, "MATH-M 211", "Calculus I", "C+", "Fall 2023");

        //Spring 2024
        addCourse(s1, "CSCI-C 212", "Intro to Software Systems", "A-", "Spring 2024");
        addCourse(s1, "FOLK-F 101", "Introduction to Folklore", "B+", "Spring 2024");
        addCourse(s1, "INFO-I 101", "Introduction to Informatics", "A+", "Spring 2024");
        addCourse(s1, "LLLC-Y 102", "Tech Leadership & Innovation II", "A+", "Spring 2024");
        addCourse(s1, "MSCH-C 101", "Media", "A+", "Spring 2024");
        addCourse(s1, "MATH-M 212", "Calculus II", "W", "Spring 2024"); 

        //Fall 2024
        addCourse(s1, "BUS-K 201", "The Computer in Business", "B", "Fall 2024");
        addCourse(s1, "CSCI-C 241", "Discrete Structures for CSCI", "C", "Fall 2024");
        addCourse(s1, "FOLK-F 131", "Folklore in the United States", "B+", "Fall 2024");
        addCourse(s1, "INFO-I 368", "Intro to Network Science", "C+", "Fall 2024");
        addCourse(s1, "INTL-I 100", "Intro to International Studies", "B", "Fall 2024");

        //Spring 2025
        addCourse(s1, "BUS-A 200", "Accounting Non-Business Majors", "B+", "Spring 2025");
        addCourse(s1, "CSCI-C 343", "Data Structures", "C-", "Spring 2025");
        addCourse(s1, "CSCI-Y 395", "Career Develpt for CSCI Majors", "A-", "Spring 2025");
        addCourse(s1, "INFO-I 360", "Web Design", "A", "Spring 2025");
        addCourse(s1, "MELC-M 204", "Topics in Mid East Culture & Soc", "A", "Spring 2025");
        addCourse(s1, "PHYS-P 120", "Energy and Technology", "A", "Spring 2025");

        //Fall 2025
        addCourse(s1, "AST-A 100", "The Solar System", "A", "Fall 2025");
        addCourse(s1, "BUS-J 306", "Strategic Mgmt & Leadership", "B+", "Fall 2025");
        addCourse(s1, "BUS-L 201", "Legal Environment of Business", "C+", "Fall 2025");
        addCourse(s1, "CSCI-B 461", "Database Concepts", "B-", "Fall 2025");
        addCourse(s1, "CSCI-P 465", "Software Eng for Info Sys I", "A+", "Fall 2025");
        addCourse(s1, "CSCI-Y 390", "Undergraduate Indpt Study", "A", "Fall 2025");
        addCourse(s1, "HIST-J 300", "Writing in History (Business in China)", "A-", "Fall 2025");

        //Spring 2026
        addCourse(s1, "AST-A 105", "Stars and Galaxies", "NR", "Spring 2026");
        addCourse(s1, "BUS-G 300", "Intro to Mgrl Econ and Strat", "NR", "Spring 2026");
        addCourse(s1, "CSCI-C 323", "Mobile App Development", "NR", "Spring 2026");
        addCourse(s1, "CSCI-P 466", "Software Eng for Info Sys II", "NR", "Spring 2026");
        addCourse(s1, "INFO-I 304", "Introduction Virtual Reality", "NR", "Spring 2026");
        addCourse(s1, "MUS-A 111", "Electronics I", "NR", "Spring 2026");
    }

    private void addCourse(Student student, String code, String title, String grade, String term) {
        Course c = new Course();
        c.setCourseCode(code);
        c.setTitle(title);
        c.setGrade(grade);
        c.setTerm(term);
        c.setStudentId(student.getId());
        courseRepo.save(c);
    }
}
