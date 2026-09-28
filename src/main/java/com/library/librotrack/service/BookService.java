package com.library.librotrack.service;

import com.library.librotrack.dto.BookRequest;
import com.library.librotrack.dto.BookResponse;
import com.library.librotrack.entity.Book;
import com.library.librotrack.entity.IssueStatus;
import com.library.librotrack.exception.InvalidOperationException;
import com.library.librotrack.exception.ResourceNotFoundException;
import com.library.librotrack.repository.BookRepository;
import com.library.librotrack.repository.IssueRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookService {

    private final BookRepository bookRepository;
    private final IssueRecordRepository issueRecordRepository;

    public BookService(BookRepository bookRepository, IssueRecordRepository issueRecordRepository) {
        this.bookRepository = bookRepository;
        this.issueRecordRepository = issueRecordRepository;
    }

    public BookResponse createBook(BookRequest request) {
        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new InvalidOperationException("A book with ISBN '" + request.getIsbn() + "' already exists.");
        }

        if (request.getTotalCopies() == null || request.getTotalCopies() <= 0) {
            throw new InvalidOperationException("Total copies must be greater than zero.");
        }

        Book book = new Book();
        book.setTitle(request.getTitle().trim());
        book.setAuthor(request.getAuthor().trim());
        book.setIsbn(request.getIsbn().trim());
        book.setCategory(request.getCategory().trim());
        book.setTotalCopies(request.getTotalCopies());
        book.setAvailableCopies(request.getTotalCopies());

        Book saved = bookRepository.save(book);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BookResponse> getAllBooks() {
        return bookRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BookResponse getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));
        return toResponse(book);
    }

    public BookResponse updateBook(Long id, BookRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));

        if (bookRepository.existsByIsbnAndIdNot(request.getIsbn().trim(), id)) {
            throw new InvalidOperationException("Another book with ISBN '" + request.getIsbn() + "' already exists.");
        }

        if (request.getTotalCopies() == null || request.getTotalCopies() <= 0) {
            throw new InvalidOperationException("Total copies must be greater than zero.");
        }

        int currentlyIssued = book.getTotalCopies() - book.getAvailableCopies();
        if (request.getTotalCopies() < currentlyIssued) {
            throw new InvalidOperationException("Cannot reduce total copies to " + request.getTotalCopies() + 
                    " because " + currentlyIssued + " copies are currently issued.");
        }

        book.setTitle(request.getTitle().trim());
        book.setAuthor(request.getAuthor().trim());
        book.setIsbn(request.getIsbn().trim());
        book.setCategory(request.getCategory().trim());
        book.setTotalCopies(request.getTotalCopies());
        book.setAvailableCopies(request.getTotalCopies() - currentlyIssued);

        Book updated = bookRepository.save(book);
        return toResponse(updated);
    }

    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));

        if (issueRecordRepository.existsByBookIdAndStatus(id, IssueStatus.ISSUED)) {
            throw new InvalidOperationException("Cannot delete book '" + book.getTitle() + "' because copies are currently issued.");
        }

        bookRepository.delete(book);
    }

    @Transactional(readOnly = true)
    public List<BookResponse> searchBooks(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllBooks();
        }
        return bookRepository.searchBooks(query.trim())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public BookResponse toResponse(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getCategory(),
                book.getTotalCopies(),
                book.getAvailableCopies()
        );
    }
}
