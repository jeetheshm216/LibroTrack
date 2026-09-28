package com.library.librotrack.controller;

import com.library.librotrack.dto.IssueBookRequest;
import com.library.librotrack.dto.IssueRecordResponse;
import com.library.librotrack.service.IssueRecordService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/issues")
public class IssueRecordController {

    private final IssueRecordService issueRecordService;

    public IssueRecordController(IssueRecordService issueRecordService) {
        this.issueRecordService = issueRecordService;
    }

    @PostMapping
    public ResponseEntity<IssueRecordResponse> issueBook(@Valid @RequestBody IssueBookRequest request) {
        IssueRecordResponse response = issueRecordService.issueBook(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<IssueRecordResponse>> getAllIssues() {
        return ResponseEntity.ok(issueRecordService.getAllIssues());
    }

    @GetMapping("/active")
    public ResponseEntity<List<IssueRecordResponse>> getActiveIssues() {
        return ResponseEntity.ok(issueRecordService.getActiveIssues());
    }

    @GetMapping("/{id}")
    public ResponseEntity<IssueRecordResponse> getIssueById(@PathVariable Long id) {
        return ResponseEntity.ok(issueRecordService.getIssueById(id));
    }

    @PutMapping("/{id}/return")
    public ResponseEntity<IssueRecordResponse> returnBook(@PathVariable Long id) {
        return ResponseEntity.ok(issueRecordService.returnBook(id));
    }
}
