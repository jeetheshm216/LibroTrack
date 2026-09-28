package com.library.librotrack.config;

import com.library.librotrack.entity.Book;
import com.library.librotrack.entity.Student;
import com.library.librotrack.repository.BookRepository;
import com.library.librotrack.repository.StudentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final BookRepository bookRepository;
    private final StudentRepository studentRepository;

    public DataInitializer(BookRepository bookRepository, StudentRepository studentRepository) {
        this.bookRepository = bookRepository;
        this.studentRepository = studentRepository;
    }

    @Override
    public void run(String... args) {
        if (bookRepository.count() == 0) {
            List<Book> initialBooks = List.of(
                    new Book(null, "Clean Code: A Handbook of Agile Software Craftsmanship", "Robert C. Martin", "978-0132350884", "Computer Science", 5, 5),
                    new Book(null, "Introduction to Algorithms (4th Edition)", "Thomas H. Cormen, Charles E. Leiserson", "978-0262046305", "Computer Science", 4, 4),
                    new Book(null, "Design Patterns: Elements of Reusable Object-Oriented Software", "Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides", "978-0201633610", "Software Engineering", 3, 3),
                    new Book(null, "Artificial Intelligence: A Modern Approach (4th Edition)", "Stuart Russell, Peter Norvig", "978-0136042594", "Artificial Intelligence", 4, 4),
                    new Book(null, "Database System Concepts (7th Edition)", "Abraham Silberschatz, Henry F. Korth", "978-0078022159", "Database Systems", 5, 5),
                    new Book(null, "Computer Networking: A Top-Down Approach (8th Edition)", "James Kurose, Keith Ross", "978-0133594140", "Computer Networks", 4, 4)
            );
            bookRepository.saveAll(initialBooks);
        }

        if (studentRepository.count() == 0) {
            List<Student> initialStudents = List.of(
                    new Student(null, "Aarav Sharma", "2024CS101", "aarav.sharma@college.edu", "Computer Science"),
                    new Student(null, "Diya Patel", "2024CS102", "diya.patel@college.edu", "Computer Science"),
                    new Student(null, "Rohan Verma", "2024IT201", "rohan.verma@college.edu", "Information Technology"),
                    new Student(null, "Sneha Kulkarni", "2024EC301", "sneha.kulkarni@college.edu", "Electronics & Communication")
            );
            studentRepository.saveAll(initialStudents);
        }
    }
}
