package com.library.librotrack.service;

import com.library.librotrack.dto.StudentRequest;
import com.library.librotrack.dto.StudentResponse;
import com.library.librotrack.entity.IssueStatus;
import com.library.librotrack.entity.Student;
import com.library.librotrack.exception.InvalidOperationException;
import com.library.librotrack.exception.ResourceNotFoundException;
import com.library.librotrack.repository.IssueRecordRepository;
import com.library.librotrack.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final IssueRecordRepository issueRecordRepository;

    public StudentService(StudentRepository studentRepository, IssueRecordRepository issueRecordRepository) {
        this.studentRepository = studentRepository;
        this.issueRecordRepository = issueRecordRepository;
    }

    public StudentResponse createStudent(StudentRequest request) {
        if (studentRepository.existsByRegisterNumber(request.getRegisterNumber().trim())) {
            throw new InvalidOperationException("A student with Register Number '" + request.getRegisterNumber() + "' already exists.");
        }

        if (studentRepository.existsByEmail(request.getEmail().trim())) {
            throw new InvalidOperationException("A student with email '" + request.getEmail() + "' already exists.");
        }

        Student student = new Student();
        student.setName(request.getName().trim());
        student.setRegisterNumber(request.getRegisterNumber().trim());
        student.setEmail(request.getEmail().trim());
        student.setDepartment(request.getDepartment().trim());

        Student saved = studentRepository.save(student);
        return toResponse(saved, 0L);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getAllStudents() {
        return studentRepository.findAll()
                .stream()
                .map(student -> {
                    long activeCount = issueRecordRepository.countByStudentIdAndStatus(student.getId(), IssueStatus.ISSUED);
                    return toResponse(student, activeCount);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));
        long activeCount = issueRecordRepository.countByStudentIdAndStatus(student.getId(), IssueStatus.ISSUED);
        return toResponse(student, activeCount);
    }

    @Transactional(readOnly = true)
    public Student getStudentEntity(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + id));
    }

    public StudentResponse toResponse(Student student, Long activeCount) {
        return new StudentResponse(
                student.getId(),
                student.getName(),
                student.getRegisterNumber(),
                student.getEmail(),
                student.getDepartment(),
                activeCount != null ? activeCount : 0L
        );
    }
}
