package com.library.librotrack.dto;

public class StudentResponse {

    private Long id;
    private String name;
    private String registerNumber;
    private String email;
    private String department;
    private Long activeIssuesCount;

    public StudentResponse() {
    }

    public StudentResponse(Long id, String name, String registerNumber, String email, String department, Long activeIssuesCount) {
        this.id = id;
        this.name = name;
        this.registerNumber = registerNumber;
        this.email = email;
        this.department = department;
        this.activeIssuesCount = activeIssuesCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegisterNumber() {
        return registerNumber;
    }

    public void setRegisterNumber(String registerNumber) {
        this.registerNumber = registerNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Long getActiveIssuesCount() {
        return activeIssuesCount;
    }

    public void setActiveIssuesCount(Long activeIssuesCount) {
        this.activeIssuesCount = activeIssuesCount;
    }
}
