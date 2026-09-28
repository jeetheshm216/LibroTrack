package com.library.librotrack;

import com.library.librotrack.dto.BookRequest;
import com.library.librotrack.dto.BookResponse;
import com.library.librotrack.dto.IssueBookRequest;
import com.library.librotrack.dto.IssueRecordResponse;
import com.library.librotrack.dto.StudentRequest;
import com.library.librotrack.dto.StudentResponse;
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
import com.library.librotrack.service.BookService;
import com.library.librotrack.service.IssueRecordService;
import com.library.librotrack.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibrotrackServiceTests {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private IssueRecordRepository issueRecordRepository;

    @InjectMocks
    private BookService bookService;

    @InjectMocks
    private StudentService studentService;

    @InjectMocks
    private IssueRecordService issueRecordService;

    private Book testBook;
    private Student testStudent;

    @BeforeEach
    void setUp() {
        testBook = new Book(1L, "Clean Code", "Robert C. Martin", "978-0132350884", "Computer Science", 5, 5);
        testStudent = new Student(1L, "Alice Johnson", "REG2026001", "alice@college.edu", "CSE");
    }

    @Test
    @DisplayName("Issue Book successfully reduces available copies and sets 14-day due date")
    void testIssueBookSuccess() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(issueRecordRepository.save(any(IssueRecord.class))).thenAnswer(invocation -> {
            IssueRecord r = invocation.getArgument(0);
            r.setId(100L);
            return r;
        });

        IssueBookRequest request = new IssueBookRequest(1L, 1L);
        IssueRecordResponse response = issueRecordService.issueBook(request);

        assertNotNull(response);
        assertEquals(4, testBook.getAvailableCopies());
        assertEquals(LocalDate.now(), response.getIssueDate());
        assertEquals(LocalDate.now().plusDays(14), response.getDueDate());
        assertEquals(IssueStatus.ISSUED, response.getStatus());
        assertEquals(0.0, response.getFineAmount());
    }

    @Test
    @DisplayName("Rule 1: Cannot issue unavailable book (availableCopies == 0)")
    void testIssueUnavailableBookThrowsException() {
        testBook.setAvailableCopies(0);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(testStudent));

        IssueBookRequest request = new IssueBookRequest(1L, 1L);
        BookUnavailableException ex = assertThrows(BookUnavailableException.class, () -> issueRecordService.issueBook(request));
        assertEquals("Book is currently unavailable. All copies are already issued.", ex.getMessage());
    }

    @Test
    @DisplayName("Return Book on or before due date calculates fine = 0 and increments availableCopies")
    void testReturnBookOnTime() {
        IssueRecord record = new IssueRecord(100L, testStudent, testBook, LocalDate.now().minusDays(5), LocalDate.now().plusDays(9), null, 0.0, IssueStatus.ISSUED);
        testBook.setAvailableCopies(4);

        when(issueRecordRepository.findById(100L)).thenReturn(Optional.of(record));
        when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));
        when(issueRecordRepository.save(any(IssueRecord.class))).thenAnswer(i -> i.getArgument(0));

        IssueRecordResponse response = issueRecordService.returnBook(100L);

        assertEquals(IssueStatus.RETURNED, response.getStatus());
        assertEquals(0.0, response.getFineAmount());
        assertEquals(5, testBook.getAvailableCopies());
        assertEquals(LocalDate.now(), response.getReturnDate());
    }

    @Test
    @DisplayName("Return Book after due date calculates fine = lateDays * 5")
    void testReturnBookLateCalculatesFine() {
        // Due date was 4 days ago
        LocalDate dueDate = LocalDate.now().minusDays(4);
        LocalDate issueDate = dueDate.minusDays(14);
        IssueRecord record = new IssueRecord(100L, testStudent, testBook, issueDate, dueDate, null, 0.0, IssueStatus.ISSUED);
        testBook.setAvailableCopies(4);

        when(issueRecordRepository.findById(100L)).thenReturn(Optional.of(record));
        when(bookRepository.save(any(Book.class))).thenAnswer(i -> i.getArgument(0));
        when(issueRecordRepository.save(any(IssueRecord.class))).thenAnswer(i -> i.getArgument(0));

        IssueRecordResponse response = issueRecordService.returnBook(100L);

        assertEquals(IssueStatus.RETURNED, response.getStatus());
        assertEquals(20.0, response.getFineAmount()); // 4 days * 5 = 20.0
        assertEquals(5, testBook.getAvailableCopies());
    }

    @Test
    @DisplayName("Return already returned book throws InvalidOperationException")
    void testReturnAlreadyReturnedBook() {
        IssueRecord record = new IssueRecord(100L, testStudent, testBook, LocalDate.now().minusDays(10), LocalDate.now().plusDays(4), LocalDate.now().minusDays(2), 0.0, IssueStatus.RETURNED);

        when(issueRecordRepository.findById(100L)).thenReturn(Optional.of(record));

        assertThrows(InvalidOperationException.class, () -> issueRecordService.returnBook(100L));
    }

    @Test
    @DisplayName("Nonexistent book throws ResourceNotFoundException")
    void testNonexistentBook() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookService.getBookById(999L));
    }

    @Test
    @DisplayName("Nonexistent student throws ResourceNotFoundException")
    void testNonexistentStudent() {
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studentService.getStudentById(999L));
    }

    @Test
    @DisplayName("Duplicate ISBN on creation throws InvalidOperationException")
    void testDuplicateIsbnThrowsException() {
        when(bookRepository.existsByIsbn("978-0132350884")).thenReturn(true);

        BookRequest request = new BookRequest("Clean Code", "Robert Martin", "978-0132350884", "CS", 5);
        assertThrows(InvalidOperationException.class, () -> bookService.createBook(request));
    }
}
