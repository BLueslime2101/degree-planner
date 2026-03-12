package com.example.degreeplanner.domain;
import jakarta.persistence.*;

@Entity
public class Student { 
    // The @Id annotation indicates that the 'id' field is the primary key of the Student entity,
    @Id 
    // @GeneratedValue with strategy AUTO means that the database will automatically generate a unique value for this field when a new Student is created.
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    // Ensure usernames are unique at the database level to prevent duplicate login records.
    @Column(unique = true)
    private String studentUsername; // Example: "Shisara"
    private String currentSemester; // Example: "Fall 2024"

    // Getters and setters for the Student entity fields
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    } 
    
    // 
    public String getStudentUsername() { return studentUsername; }
    public void setStudentUsername(String studentUsername) { this.studentUsername = studentUsername; }
    public String getCurrentSemester() { return currentSemester; }
    public void setCurrentSemester(String s) { this.currentSemester = s; }
}