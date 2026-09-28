package com.library.librotrack.repository;

import com.library.librotrack.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByRegisterNumber(String registerNumber);

    boolean existsByRegisterNumberAndIdNot(String registerNumber, Long id);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<Student> findByRegisterNumber(String registerNumber);
}
