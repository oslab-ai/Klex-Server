package com.Klex.reportingService.repository;

import com.Klex.reportingService.entity.ReportAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportAuditLogRepository extends JpaRepository<ReportAuditLog, Long> {

    /** All logs, newest first. */
    List<ReportAuditLog> findAllByOrderByTimestampDesc();

    /** Filter by status (SUCCESS / ERROR). */
    List<ReportAuditLog> findByStatusOrderByTimestampDesc(String status);

    /** Search by report path keyword. */
    List<ReportAuditLog> findByReportPathContainingIgnoreCaseOrderByTimestampDesc(String keyword);

    /** Filter by output format (PDF, XLSX, etc.). */
    List<ReportAuditLog> findByOutputFormatIgnoreCaseOrderByTimestampDesc(String format);

    /** Filter by data source type (jdbc, csv, json, xml, inmemory). */
    List<ReportAuditLog> findByDataSourceTypeIgnoreCaseOrderByTimestampDesc(String dataSourceType);

    /** Logs within a date range. */
    List<ReportAuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime from, LocalDateTime to);

    /** Count reports by status. */
    long countByStatus(String status);

    /** Average total execution time across all runs. */
    @Query("SELECT AVG(a.totalExecutionTimeMs) FROM ReportAuditLog a WHERE a.totalExecutionTimeMs IS NOT NULL")
    Double findAverageExecutionTimeMs();

    /** Average compile time. */
    @Query("SELECT AVG(a.compileTimeMs) FROM ReportAuditLog a WHERE a.compileTimeMs IS NOT NULL")
    Double findAverageCompileTimeMs();

    /** Average fill time. */
    @Query("SELECT AVG(a.fillTimeMs) FROM ReportAuditLog a WHERE a.fillTimeMs IS NOT NULL")
    Double findAverageFillTimeMs();

    /** Average export time. */
    @Query("SELECT AVG(a.exportTimeMs) FROM ReportAuditLog a WHERE a.exportTimeMs IS NOT NULL")
    Double findAverageExportTimeMs();

    /** Total output data volume in bytes. */
    @Query("SELECT COALESCE(SUM(a.outputFileSizeBytes), 0) FROM ReportAuditLog a WHERE a.outputFileSizeBytes IS NOT NULL")
    Long findTotalDataVolumeBytes();

    /** Slowest reports (top N by total execution time). */
    List<ReportAuditLog> findTop10ByOrderByTotalExecutionTimeMsDesc();
}
