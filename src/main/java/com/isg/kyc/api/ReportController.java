package com.isg.kyc.api;

import com.isg.kyc.service.ExcelReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ExcelReportService excelReportService;

    public ReportController(ExcelReportService excelReportService) {
        this.excelReportService = excelReportService;
    }

    /**
     * Full KYC report (all sheets).
     * GET /api/reports/kyc.xlsx
     */
    @GetMapping(value = "/kyc.xlsx", produces = XLSX_MIME)
    public ResponseEntity<byte[]> downloadFullReport() throws IOException {
        log.info("📥 Full KYC report requested");
        byte[] xlsx = excelReportService.generateFailedPendingReport();
        return buildResponse(xlsx, "kyc-report");
    }

    /**
     * Failed + Pending report only.
     * GET /api/reports/failed-pending.xlsx
     */
    @GetMapping(value = "/failed-pending.xlsx", produces = XLSX_MIME)
    public ResponseEntity<byte[]> downloadFailedPendingReport() throws IOException {
        log.info("📥 Failed/Pending report requested");
        byte[] xlsx = excelReportService.generateFailedPendingReport();
        return buildResponse(xlsx, "failed-pending-report");
    }

    private ResponseEntity<byte[]> buildResponse(byte[] body, String prefix) {
        String filename = prefix + "-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) +
                ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(XLSX_MIME))
                .body(body);
    }
}