package com.library.librotrack.repository;

import com.library.librotrack.entity.IssueRecord;
import com.library.librotrack.entity.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IssueRecordRepository extends JpaRepository<IssueRecord, Long> {

    @Query("SELECT i FROM IssueRecord i JOIN FETCH i.book JOIN FETCH i.student ORDER BY i.id DESC")
    List<IssueRecord> findAllWithDetails();

    @Query("SELECT i FROM IssueRecord i JOIN FETCH i.book JOIN FETCH i.student WHERE i.status = :status ORDER BY i.dueDate ASC")
    List<IssueRecord> findByStatusWithDetails(@Param("status") IssueStatus status);

    @Query("SELECT i FROM IssueRecord i JOIN FETCH i.book JOIN FETCH i.student WHERE i.student.id = :studentId ORDER BY i.id DESC")
    List<IssueRecord> findByStudentIdWithDetails(@Param("studentId") Long studentId);

    @Query("SELECT i FROM IssueRecord i JOIN FETCH i.book JOIN FETCH i.student WHERE i.student.id = :studentId AND i.status = :status ORDER BY i.id DESC")
    List<IssueRecord> findByStudentIdAndStatusWithDetails(@Param("studentId") Long studentId, @Param("status") IssueStatus status);

    @Query("SELECT i FROM IssueRecord i JOIN FETCH i.book JOIN FETCH i.student WHERE i.book.id = :bookId ORDER BY i.id DESC")
    List<IssueRecord> findByBookIdWithDetails(@Param("bookId") Long bookId);

    long countByStatus(IssueStatus status);

    long countByDueDateBeforeAndStatus(LocalDate date, IssueStatus status);

    long countByStudentIdAndStatus(Long studentId, IssueStatus status);

    boolean existsByBookIdAndStatus(Long bookId, IssueStatus status);

    @Query("SELECT COALESCE(SUM(i.fineAmount), 0.0) FROM IssueRecord i")
    Double sumTotalFines();
}
