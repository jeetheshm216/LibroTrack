package com.library.librotrack.service;

import com.library.librotrack.dto.DashboardStatsResponse;
import com.library.librotrack.entity.IssueStatus;
import com.library.librotrack.repository.BookRepository;
import com.library.librotrack.repository.IssueRecordRepository;
import com.library.librotrack.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final BookRepository bookRepository;
    private final StudentRepository studentRepository;
    private final IssueRecordRepository issueRecordRepository;

    public DashboardService(BookRepository bookRepository,
                            StudentRepository studentRepository,
                            IssueRecordRepository issueRecordRepository) {
        this.bookRepository = bookRepository;
        this.studentRepository = studentRepository;
        this.issueRecordRepository = issueRecordRepository;
    }

    public DashboardStatsResponse getDashboardStats() {
        long totalBooks = bookRepository.count();
        long totalStudents = studentRepository.count();
        long totalCopies = bookRepository.sumTotalCopies() != null ? bookRepository.sumTotalCopies() : 0;
        long availableCopies = bookRepository.sumAvailableCopies() != null ? bookRepository.sumAvailableCopies() : 0;
        long issuedBooks = issueRecordRepository.countByStatus(IssueStatus.ISSUED);
        long overdueBooks = issueRecordRepository.countByDueDateBeforeAndStatus(LocalDate.now(), IssueStatus.ISSUED);
        double totalFines = issueRecordRepository.sumTotalFines() != null ? issueRecordRepository.sumTotalFines() : 0.0;

        return new DashboardStatsResponse(
                totalBooks,
                totalStudents,
                totalCopies,
                availableCopies,
                issuedBooks,
                overdueBooks,
                totalFines
        );
    }
}
