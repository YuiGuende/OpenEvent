package com.group02.openevent.repository;

import com.group02.openevent.model.report.Report;
import com.group02.openevent.model.report.ReportStatus;
import com.group02.openevent.model.report.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IReportRepo extends JpaRepository<Report, Long> {
    
    List<Report> findByStatus(ReportStatus status);
    List<Report> findByType(ReportType type);
    List<Report> findByEventId(Long eventId);
    List<Report> findByUserId(Long userId);
    
    // Pageable listing
    Page<Report> findAll(Pageable pageable);
    Page<Report> findByStatus(ReportStatus status, Pageable pageable);
    Page<Report> findByType(ReportType type, Pageable pageable);
    Page<Report> findByStatusAndType(ReportStatus status, ReportType type, Pageable pageable);
}
