package com.Klex.reportingService.service;

import com.Klex.reportingService.dto.AuditLogResponse;
import com.Klex.reportingService.entity.ReportAuditLog;
import com.Klex.reportingService.repository.ReportAuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    @Autowired
    private ReportAuditLogRepository repository;

    // ── Logging ──

    /**
     * Persist a new audit log entry.
     */
    public ReportAuditLog logReportExecution(ReportAuditLog log) {
        return repository.save(log);
    }

    // ── Query: all logs ──

    public List<AuditLogResponse> getAllLogs() {
        return AuditLogResponse.fromEntities(repository.findAllByOrderByTimestampDesc());
    }

    // ── Query: filter by status ──

    public List<AuditLogResponse> getLogsByStatus(String status) {
        return AuditLogResponse.fromEntities(
                repository.findByStatusOrderByTimestampDesc(status.toUpperCase()));
    }

    // ── Query: search by report path ──

    public List<AuditLogResponse> searchByReportPath(String keyword) {
        return AuditLogResponse.fromEntities(
                repository.findByReportPathContainingIgnoreCaseOrderByTimestampDesc(keyword));
    }

    // ── Query: filter by output format ──

    public List<AuditLogResponse> getLogsByFormat(String format) {
        return AuditLogResponse.fromEntities(
                repository.findByOutputFormatIgnoreCaseOrderByTimestampDesc(format));
    }

    // ── Query: filter by data source type ──

    public List<AuditLogResponse> getLogsByDataSourceType(String dataSourceType) {
        return AuditLogResponse.fromEntities(
                repository.findByDataSourceTypeIgnoreCaseOrderByTimestampDesc(dataSourceType));
    }

    // ── Aggregate: performance statistics ──

    public AuditLogResponse.PerformanceStats getPerformanceStats() {
        AuditLogResponse.PerformanceStats stats = new AuditLogResponse.PerformanceStats();

        long total = repository.count();
        long success = repository.countByStatus("SUCCESS");
        long errors = repository.countByStatus("ERROR");

        stats.setTotalReports(total);
        stats.setSuccessCount(success);
        stats.setErrorCount(errors);
        stats.setErrorRate(total > 0 ? (double) errors / total * 100.0 : 0.0);

        stats.setAvgTotalExecutionTimeMs(repository.findAverageExecutionTimeMs());
        stats.setAvgCompileTimeMs(repository.findAverageCompileTimeMs());
        stats.setAvgFillTimeMs(repository.findAverageFillTimeMs());
        stats.setAvgExportTimeMs(repository.findAverageExportTimeMs());
        stats.setTotalDataVolumeBytes(repository.findTotalDataVolumeBytes());

        // Top 10 slowest reports
        stats.setSlowestReports(
                AuditLogResponse.fromEntities(repository.findTop10ByOrderByTotalExecutionTimeMsDesc()));

        return stats;
    }
}
