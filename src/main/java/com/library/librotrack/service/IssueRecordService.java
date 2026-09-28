package com.library.librotrack.service;

import com.library.librotrack.dto.IssueBookRequest;
import com.library.librotrack.dto.IssueRecordResponse;
import com.library.librotrack.entity.Book;
import com.library.librotrack.entity.IssueRecord;
import com.library.librotrack.entity.IssueStatus;
import com.library.librotrack.entity.Student;
import com.library.librotrack.exception.BookUnavailableException;
import com.library.librotrack.exception.InvalidOperationException;
import com.library.librotrack.exception.ResourceNotFoundException;
import com.library.librotrack.repository.BookRepository;
import com.library.librotrack.repository.IssueRecordRepository;
import com.library.librotrack.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class IssueRecordService {

    public static final double FINE_PER_DAY = 5.0;
    public static final int ISSUE_PERIOD_DAYS = 14;

    private final IssueRecordRepository issueRecordRepository;
    private final BookRepository bookRepository;
    private final StudentRepository studentRepository;

    public IssueRecordService(IssueRecordRepository issueRecordRepository,
                              BookRepository bookRepository,
                              StudentRepository studentRepository) {
        this.issueRecordRepository = issueRecordRepository;
        this.bookRepository = bookRepository;
        this.studentRepository = studentRepository;
    }

    public IssueRecordResponse issueBook(IssueBookRequest request) {
        if (request.getBookId() == null) {
            throw new InvalidOperationException("Book ID must not be null.");
        }
        if (request.getStudentId() == null) {
            throw new InvalidOperationException("Student ID must not be null.");
        }

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + request.getBookId()));

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + request.getStudentId()));

        // Rule 1: Cannot issue unavailable book (checked in service layer)
        if (book.getAvailableCopies() == null || book.getAvailableCopies() <= 0) {
            throw new BookUnavailableException("Book is currently unavailable. All copies are already issued.");
        }

        // Rule 2: 14-day issue period calculated by backend
        LocalDate issueDate = LocalDate.now();
        LocalDate dueDate = issueDate.plusDays(ISSUE_PERIOD_DAYS);

        // Rule 3: Reduce available copies
        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        IssueRecord issueRecord = new IssueRecord();
        issueRecord.setBook(book);
        issueRecord.setStudent(student);
        issueRecord.setIssueDate(issueDate);
        issueRecord.setDueDate(dueDate);
        issueRecord.setReturnDate(null);
        issueRecord.setFineAmount(0.0);
        issueRecord.setStatus(IssueStatus.ISSUED);

        IssueRecord saved = issueRecordRepository.save(issueRecord);
        return toResponse(saved);
    }

    public IssueRecordResponse returnBook(Long issueId) {
        IssueRecord issueRecord = issueRecordRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue record not found with ID: " + issueId));

        // Rule 4: Ensure the same issue cannot be returned twice
        if (issueRecord.getStatus() == IssueStatus.RETURNED) {
            throw new InvalidOperationException("This book has already been returned on " + issueRecord.getReturnDate() + ".");
        }

        LocalDate returnDate = LocalDate.now();
        issueRecord.setReturnDate(returnDate);

        // Rule 5: Fine calculation (₹5 per day overdue)
        double fine = 0.0;
        if (returnDate.isAfter(issueRecord.getDueDate())) {
            long lateDays = ChronoUnit.DAYS.between(issueRecord.getDueDate(), returnDate);
            fine = lateDays * FINE_PER_DAY;
        }
        issueRecord.setFineAmount(fine);
        issueRecord.setStatus(IssueStatus.RETURNED);

        // Rule 4 & 26: Increase available copies, not exceeding totalCopies
        Book book = issueRecord.getBook();
        int newAvailable = Math.min(book.getAvailableCopies() + 1, book.getTotalCopies());
        book.setAvailableCopies(newAvailable);
        bookRepository.save(book);

        IssueRecord updated = issueRecordRepository.save(issueRecord);
        return toResponse(updated);
    }

    @Transactional(readOnly = true)
    public List<IssueRecordResponse> getAllIssues() {
        return issueRecordRepository.findAllWithDetails()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IssueRecordResponse> getActiveIssues() {
        return issueRecordRepository.findByStatusWithDetails(IssueStatus.ISSUED)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IssueRecordResponse getIssueById(Long id) {
        IssueRecord record = issueRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue record not found with ID: " + id));
        return toResponse(record);
    }

    @Transactional(readOnly = true)
    public List<IssueRecordResponse> getIssuesByStudentId(Long studentId) {
        // Verify student exists
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with ID: " + studentId);
        }
        return issueRecordRepository.findByStudentIdWithDetails(studentId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<IssueRecordResponse> getActiveIssuesByStudentId(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student not found with ID: " + studentId);
        }
        return issueRecordRepository.findByStudentIdAndStatusWithDetails(studentId, IssueStatus.ISSUED)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public IssueRecordResponse toResponse(IssueRecord record) {
        LocalDate today = LocalDate.now();
        long lateDays = 0;
        long daysRemaining = 0;
        boolean isOverdue = false;

        if (record.getStatus() == IssueStatus.ISSUED) {
            if (today.isAfter(record.getDueDate())) {
                isOverdue = true;
                lateDays = ChronoUnit.DAYS.between(record.getDueDate(), today);
                daysRemaining = 0;
            } else {
                isOverdue = false;
                lateDays = 0;
                daysRemaining = ChronoUnit.DAYS.between(today, record.getDueDate());
            }
        } else {
            // Already returned
            if (record.getReturnDate() != null && record.getReturnDate().isAfter(record.getDueDate())) {
                isOverdue = true;
                lateDays = ChronoUnit.DAYS.between(record.getDueDate(), record.getReturnDate());
            } else {
                isOverdue = false;
                lateDays = 0;
            }
            daysRemaining = 0;
        }

        IssueRecordResponse resp = new IssueRecordResponse();
        resp.setId(record.getId());
        resp.setBookId(record.getBook().getId());
        resp.setBookTitle(record.getBook().getTitle());
        resp.setBookAuthor(record.getBook().getAuthor());
        resp.setBookIsbn(record.getBook().getIsbn());
        resp.setStudentId(record.getStudent().getId());
        resp.setStudentName(record.getStudent().getName());
        resp.setStudentRegisterNumber(record.getStudent().getRegisterNumber());
        resp.setIssueDate(record.getIssueDate());
        resp.setDueDate(record.getDueDate());
        resp.setReturnDate(record.getReturnDate());
        resp.setFineAmount(record.getFineAmount());
        resp.setStatus(record.getStatus());
        resp.setLateDays(lateDays);
        resp.setDaysRemaining(daysRemaining);
        resp.setOverdue(isOverdue);

        return resp;
    }
}
