package com.library.librotrack.dto;

public class DashboardStatsResponse {

    private long totalBooks;
    private long totalStudents;
    private long totalCopies;
    private long availableCopies;
    private long issuedBooks;
    private long overdueBooks;
    private double totalFines;

    public DashboardStatsResponse() {
    }

    public DashboardStatsResponse(long totalBooks, long totalStudents, long totalCopies, long availableCopies, long issuedBooks, long overdueBooks, double totalFines) {
        this.totalBooks = totalBooks;
        this.totalStudents = totalStudents;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
        this.issuedBooks = issuedBooks;
        this.overdueBooks = overdueBooks;
        this.totalFines = totalFines;
    }

    public long getTotalBooks() {
        return totalBooks;
    }

    public void setTotalBooks(long totalBooks) {
        this.totalBooks = totalBooks;
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(long totalCopies) {
        this.totalCopies = totalCopies;
    }

    public long getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(long availableCopies) {
        this.availableCopies = availableCopies;
    }

    public long getIssuedBooks() {
        return issuedBooks;
    }

    public void setIssuedBooks(long issuedBooks) {
        this.issuedBooks = issuedBooks;
    }

    public long getOverdueBooks() {
        return overdueBooks;
    }

    public void setOverdueBooks(long overdueBooks) {
        this.overdueBooks = overdueBooks;
    }

    public double getTotalFines() {
        return totalFines;
    }

    public void setTotalFines(double totalFines) {
        this.totalFines = totalFines;
    }
}
