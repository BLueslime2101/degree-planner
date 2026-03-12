// Course entity representing a course taken by a student,
// with fields including course code, title, grade, term, and associated student ID.
package com.example.degreeplanner.domain;
import jakarta.persistence.*;

@Entity
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String courseCode; // Example: "CSCI-C 211"
    private String title;      // Example: "Intro to Computer Science"
    private String grade;      // Example: "A", "B+", "W", "NR"

    // Simple term/semester label to group courses, e.g. "Fall 2023", "Spring 2026"
    private String term;

    // Foreign key reference to the Student entity
    private Long studentId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
}
