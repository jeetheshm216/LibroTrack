package com.library.librotrack.controller;

import com.library.librotrack.dto.IssueRecordResponse;
import com.library.librotrack.dto.StudentRequest;
import com.library.librotrack.dto.StudentResponse;
import com.library.librotrack.service.IssueRecordService;
import com.library.librotrack.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final IssueRecordService issueRecordService;

    public StudentController(StudentService studentService, IssueRecordService issueRecordService) {
        this.studentService = studentService;
        this.issueRecordService = issueRecordService;
    }

    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody StudentRequest request) {
        StudentResponse created = studentService.createStudent(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<StudentResponse>> getAllStudents() {
        return ResponseEntity.ok(studentService.getAllStudents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @GetMapping("/{id}/issued-books")
    public ResponseEntity<List<IssueRecordResponse>> getStudentIssuedBooks(@PathVariable Long id) {
        return ResponseEntity.ok(issueRecordService.getActiveIssuesByStudentId(id));
    }
}
