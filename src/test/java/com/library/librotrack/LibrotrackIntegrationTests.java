package com.library.librotrack;

import com.library.librotrack.controller.BookController;
import com.library.librotrack.controller.DashboardController;
import com.library.librotrack.controller.IssueRecordController;
import com.library.librotrack.controller.StudentController;
import com.library.librotrack.dto.*;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LibrotrackIntegrationTests {

    @Autowired
    private BookController bookController;

    @Autowired
    private StudentController studentController;

    @Autowired
    private IssueRecordController issueRecordController;

    @Autowired
    private DashboardController dashboardController;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private IssueRecordRepository issueRecordRepository;

    @BeforeEach
    void cleanUp() {
        issueRecordRepository.deleteAll();
        bookRepository.deleteAll();
        studentRepository.deleteAll();
    }

    @Test
    @DisplayName("Case 1: Issue available book -> 201 Created and copies decrease")
    void testCase1_IssueAvailableBook() {
        ResponseEntity<BookResponse> bookResp = bookController.createBook(
                new BookRequest("The Pragmatic Programmer", "David Thomas", "978-0135957059", "CS", 3)
        );
        assertEquals(HttpStatus.CREATED, bookResp.getStatusCode());
        Long bookId = bookResp.getBody().getId();

        ResponseEntity<StudentResponse> studentResp = studentController.createStudent(
                new StudentRequest("John Doe", "CS101", "john@example.com", "Computer Science")
        );
        assertEquals(HttpStatus.CREATED, studentResp.getStatusCode());
        Long studentId = studentResp.getBody().getId();

        IssueBookRequest req = new IssueBookRequest(bookId, studentId);
        ResponseEntity<IssueRecordResponse> issueResp = issueRecordController.issueBook(req);

        assertEquals(HttpStatus.CREATED, issueResp.getStatusCode());
        assertNotNull(issueResp.getBody());
        assertEquals(IssueStatus.ISSUED, issueResp.getBody().getStatus());
        assertEquals(0.0, issueResp.getBody().getFineAmount());
        assertEquals(LocalDate.now().plusDays(14), issueResp.getBody().getDueDate());

        // Verify available copies decreased to 2 in database
        Book updatedBook = bookRepository.findById(bookId).orElseThrow();
        assertEquals(2, updatedBook.getAvailableCopies());
    }

    @Test
    @DisplayName("Case 2: Issue when availableCopies = 0 -> BookUnavailableException (400 Bad Request)")
    void testCase2_IssueUnavailableBook() {
        Book book = bookRepository.save(new Book(null, "Design Patterns", "GoF", "978-0201633610", "CS", 1, 0));
        Student student = studentRepository.save(new Student(null, "Jane Doe", "CS102", "jane@example.com", "Computer Science"));

        IssueBookRequest req = new IssueBookRequest(book.getId(), student.getId());

        BookUnavailableException ex = assertThrows(BookUnavailableException.class, () -> issueRecordController.issueBook(req));
        assertEquals("Book is currently unavailable. All copies are already issued.", ex.getMessage());
    }

    @Test
    @DisplayName("Case 3: Return book on or before due date -> fine = 0, copies increase")
    void testCase3_ReturnBookOnTime() {
        Book book = bookRepository.save(new Book(null, "Refactoring", "Martin Fowler", "978-0201485677", "CS", 2, 1));
        Student student = studentRepository.save(new Student(null, "Bob Smith", "CS103", "bob@example.com", "Computer Science"));

        IssueRecord record = new IssueRecord(null, student, book, LocalDate.now().minusDays(5), LocalDate.now().plusDays(9), null, 0.0, IssueStatus.ISSUED);
        record = issueRecordRepository.save(record);

        ResponseEntity<IssueRecordResponse> returnResp = issueRecordController.returnBook(record.getId());
        assertEquals(HttpStatus.OK, returnResp.getStatusCode());
        assertEquals(IssueStatus.RETURNED, returnResp.getBody().getStatus());
        assertEquals(0.0, returnResp.getBody().getFineAmount());
        assertEquals(LocalDate.now(), returnResp.getBody().getReturnDate());

        Book updatedBook = bookRepository.findById(book.getId()).orElseThrow();
        assertEquals(2, updatedBook.getAvailableCopies());
    }

    @Test
    @DisplayName("Case 4: Return book after due date -> fine = lateDays * 5")
    void testCase4_ReturnBookLateCalculatesFine() {
        Book book = bookRepository.save(new Book(null, "Database Systems", "Silberschatz", "978-0078022159", "CS", 2, 1));
        Student student = studentRepository.save(new Student(null, "Carol Danvers", "CS104", "carol@example.com", "Computer Science"));

        LocalDate dueDate = LocalDate.now().minusDays(4);
        LocalDate issueDate = dueDate.minusDays(14);
        IssueRecord record = new IssueRecord(null, student, book, issueDate, dueDate, null, 0.0, IssueStatus.ISSUED);
        record = issueRecordRepository.save(record);

        ResponseEntity<IssueRecordResponse> returnResp = issueRecordController.returnBook(record.getId());
        assertEquals(HttpStatus.OK, returnResp.getStatusCode());
        assertEquals(IssueStatus.RETURNED, returnResp.getBody().getStatus());
        assertEquals(20.0, returnResp.getBody().getFineAmount()); // 4 days * 5 = 20.0
        assertEquals(4L, returnResp.getBody().getLateDays());

        Book updatedBook = bookRepository.findById(book.getId()).orElseThrow();
        assertEquals(2, updatedBook.getAvailableCopies());
    }

    @Test
    @DisplayName("Case 5: Return an already returned book -> InvalidOperationException (400 Bad Request)")
    void testCase5_ReturnAlreadyReturnedBook() {
        Book book = bookRepository.save(new Book(null, "Operating Systems", "Tanenbaum", "978-0133591620", "CS", 2, 2));
        Student student = studentRepository.save(new Student(null, "Dave Miller", "CS105", "dave@example.com", "Computer Science"));

        IssueRecord record = new IssueRecord(null, student, book, LocalDate.now().minusDays(10), LocalDate.now().plusDays(4), LocalDate.now().minusDays(1), 0.0, IssueStatus.RETURNED);
        record = issueRecordRepository.save(record);

        Long issueId = record.getId();
        InvalidOperationException ex = assertThrows(InvalidOperationException.class, () -> issueRecordController.returnBook(issueId));
        assertTrue(ex.getMessage().contains("already been returned"));
    }

    @Test
    @DisplayName("Case 6: Request a nonexistent book -> ResourceNotFoundException (404 Not Found)")
    void testCase6_NonexistentBook() {
        assertThrows(ResourceNotFoundException.class, () -> bookController.getBookById(99999L));
    }

    @Test
    @DisplayName("Case 7: Request a nonexistent student -> ResourceNotFoundException (404 Not Found)")
    void testCase7_NonexistentStudent() {
        assertThrows(ResourceNotFoundException.class, () -> studentController.getStudentById(99999L));
    }

    @Test
    @DisplayName("Dashboard stats API returns calculated statistics from database")
    void testDashboardStats() {
        Book book = bookRepository.save(new Book(null, "Book 1", "Author 1", "ISBN-1", "Fiction", 10, 8));
        Student student = studentRepository.save(new Student(null, "Eve", "CS106", "eve@example.com", "IT"));

        issueRecordRepository.save(new IssueRecord(null, student, book, LocalDate.now().minusDays(20), LocalDate.now().minusDays(6), LocalDate.now(), 30.0, IssueStatus.RETURNED));
        issueRecordRepository.save(new IssueRecord(null, student, book, LocalDate.now().minusDays(10), LocalDate.now().plusDays(4), null, 0.0, IssueStatus.ISSUED));

        ResponseEntity<DashboardStatsResponse> statsResp = dashboardController.getDashboardStats();
        assertEquals(HttpStatus.OK, statsResp.getStatusCode());
        DashboardStatsResponse stats = statsResp.getBody();
        assertNotNull(stats);
        assertEquals(1, stats.getTotalBooks());
        assertEquals(1, stats.getTotalStudents());
        assertEquals(10, stats.getTotalCopies());
        assertEquals(8, stats.getAvailableCopies());
        assertEquals(1, stats.getIssuedBooks());
        assertEquals(30.0, stats.getTotalFines());
    }

    @Test
    @DisplayName("Book search by query checks title, author, and category case-insensitively")
    void testBookSearch() {
        bookRepository.save(new Book(null, "Artificial Intelligence: A Modern Approach", "Stuart Russell", "978-0136042594", "AI", 5, 5));
        bookRepository.save(new Book(null, "Deep Learning", "Ian Goodfellow", "978-0262035613", "Machine Learning", 4, 4));

        ResponseEntity<List<BookResponse>> search1 = bookController.searchBooks("intelligence");
        assertEquals(1, search1.getBody().size());
        assertTrue(search1.getBody().get(0).getTitle().contains("Artificial Intelligence"));

        ResponseEntity<List<BookResponse>> search2 = bookController.searchBooks("goodfellow");
        assertEquals(1, search2.getBody().size());
        assertEquals("Ian Goodfellow", search2.getBody().get(0).getAuthor());

        ResponseEntity<List<BookResponse>> search3 = bookController.searchBooks("machine learning");
        assertEquals(1, search3.getBody().size());
        assertEquals("Machine Learning", search3.getBody().get(0).getCategory());
    }
}
