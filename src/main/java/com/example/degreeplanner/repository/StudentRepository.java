package com.example.degreeplanner.repository;

import com.example.degreeplanner.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student>findFirstByStudentUsername(String studentUsername);

    // helper used by the data loader so we can remove any accidental duplicates
    List<Student> findAllByStudentUsername(String studentUsername);
}