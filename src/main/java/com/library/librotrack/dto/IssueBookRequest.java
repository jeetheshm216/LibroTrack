package com.library.librotrack.dto;

import jakarta.validation.constraints.NotNull;

public class IssueBookRequest {

    @NotNull(message = "Book ID is required")
    private Long bookId;

    @NotNull(message = "Student ID is required")
    private Long studentId;

    public IssueBookRequest() {
    }

    public IssueBookRequest(Long bookId, Long studentId) {
        this.bookId = bookId;
        this.studentId = studentId;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }
}
