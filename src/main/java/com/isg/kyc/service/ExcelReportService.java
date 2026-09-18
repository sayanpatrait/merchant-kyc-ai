package com.isg.kyc.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.isg.kyc.model.Customer;
import com.isg.kyc.model.KycStatus;
import com.isg.kyc.model.Merchant;
import com.isg.kyc.repository.CustomerRepository;
import com.isg.kyc.repository.MerchantRepository;

@Service
public class ExcelReportService {

	private static final Logger log = LoggerFactory.getLogger(ExcelReportService.class);

	private final MerchantRepository merchantRepo;
	private final CustomerRepository customerRepo;

	public ExcelReportService(MerchantRepository merchantRepo, CustomerRepository customerRepo) {
		this.merchantRepo = merchantRepo;
		this.customerRepo = customerRepo;
	}

	/**
	 * Generate a report of FAILED and PENDING merchants & customers. Returns an
	 * Excel workbook with 4 sheets.
	 */
	public byte[] generateFailedPendingReport() throws IOException {
		try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

			// Fetch and filter
			List<Merchant> failedMerchants = merchantRepo
					.findAll().stream().filter(m -> m.getPanStatus() == KycStatus.FAILED
							|| m.getAadhaarStatus() == KycStatus.FAILED || m.getGstStatus() == KycStatus.FAILED)
					.toList();

			List<Merchant> pendingMerchants = merchantRepo
					.findAll().stream().filter(m -> m.getPanStatus() == KycStatus.PENDING
							|| m.getAadhaarStatus() == KycStatus.PENDING || m.getGstStatus() == KycStatus.PENDING)
					.toList();

			List<Customer> failedCustomers = customerRepo.findAll().stream()
					.filter(c -> c.getKycStatus() == KycStatus.FAILED).toList();

			List<Customer> pendingCustomers = customerRepo.findAll().stream()
					.filter(c -> c.getKycStatus() == KycStatus.PENDING).toList();

			// Build sheets
			createMerchantSheet(workbook, "❌ Failed Merchants", failedMerchants);
			createMerchantSheet(workbook, "⏳ Pending Merchants", pendingMerchants);
			createCustomerSheet(workbook, "❌ Failed Customers", failedCustomers);
			createCustomerSheet(workbook, "⏳ Pending Customers", pendingCustomers);

			workbook.write(out);
			log.info("📊 Failed/Pending report generated — {} bytes, {} sheets", out.size(),
					workbook.getNumberOfSheets());
			return out.toByteArray();
		}
	}

	// ── Merchant sheet builder ──────────────────────────────────────
	private void createMerchantSheet(XSSFWorkbook wb, String sheetName, List<Merchant> merchants) {
		XSSFSheet sheet = wb.createSheet(sanitizeSheetName(sheetName));

		String[] headers = { "ID", "Name", "Email", "Phone", "PAN (masked)", "Aadhaar (masked)", "GSTIN", "PAN Status",
				"Aadhaar Status", "GST Status", "Created At" };

		CellStyle headerStyle = headerStyle(wb);
		CellStyle dataStyle = dataStyle(wb);
		CellStyle statusOk = statusStyle(wb, IndexedColors.LIGHT_GREEN);
		CellStyle statusPend = statusStyle(wb, IndexedColors.LIGHT_YELLOW);
		CellStyle statusFail = statusStyle(wb, IndexedColors.LIGHT_ORANGE);

		Row headerRow = sheet.createRow(0);
		for (int i = 0; i < headers.length; i++) {
			Cell c = headerRow.createCell(i);
			c.setCellValue(headers[i]);
			c.setCellStyle(headerStyle);
		}

		int rowIdx = 1;
		for (Merchant m : merchants) {
			Row r = sheet.createRow(rowIdx++);
			writeCell(r, 0, m.getId(), dataStyle);
			writeCell(r, 1, safe(m.getMerchantName()), dataStyle);
			writeCell(r, 2, safe(m.getEmail()), dataStyle);
			writeCell(r, 3, safe(m.getPhone()), dataStyle);
			writeCell(r, 4, mask(m.getPan()), dataStyle);
			writeCell(r, 5, mask(m.getAadhaar()), dataStyle);
			writeCell(r, 6, safe(m.getGstin()), dataStyle);
			writeCell(r, 7, m.getPanStatus().name(),
					statusStyleFor(m.getPanStatus(), statusOk, statusPend, statusFail));
			writeCell(r, 8, m.getAadhaarStatus().name(),
					statusStyleFor(m.getAadhaarStatus(), statusOk, statusPend, statusFail));
			writeCell(r, 9, m.getGstStatus().name(),
					statusStyleFor(m.getGstStatus(), statusOk, statusPend, statusFail));
			writeCell(r, 10, formatInstant(m.getCreatedAt()), dataStyle);
		}

		if (merchants.isEmpty()) {
			Row r = sheet.createRow(1);
			writeCell(r, 0, "No records found", dataStyle);
		}

		for (int i = 0; i < headers.length; i++) {
			sheet.autoSizeColumn(i);
			sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 800, 12000));
		}
	}

	// ── Customer sheet builder ──────────────────────────────────────
	private void createCustomerSheet(XSSFWorkbook wb, String sheetName, List<Customer> customers) {
		XSSFSheet sheet = wb.createSheet(sanitizeSheetName(sheetName));

		String[] headers = { "ID", "Full Name", "Email", "Phone", "ID Type", "ID Number (masked)", "KYC Status", "City",
				"State", "Date of Birth", "Created At" };

		CellStyle headerStyle = headerStyle(wb);
		CellStyle dataStyle = dataStyle(wb);
		CellStyle statusOk = statusStyle(wb, IndexedColors.LIGHT_GREEN);
		CellStyle statusPend = statusStyle(wb, IndexedColors.LIGHT_YELLOW);
		CellStyle statusFail = statusStyle(wb, IndexedColors.LIGHT_ORANGE);

		Row headerRow = sheet.createRow(0);
		for (int i = 0; i < headers.length; i++) {
			Cell c = headerRow.createCell(i);
			c.setCellValue(headers[i]);
			c.setCellStyle(headerStyle);
		}

		int rowIdx = 1;
		for (Customer c : customers) {
			Row r = sheet.createRow(rowIdx++);
			writeCell(r, 0, c.getId(), dataStyle);
			writeCell(r, 1, safe(c.getFullName()), dataStyle);
			writeCell(r, 2, safe(c.getEmail()), dataStyle);
			writeCell(r, 3, safe(c.getPhone()), dataStyle);
			writeCell(r, 4, c.getIdType() != null ? c.getIdType().name() : "", dataStyle);
			writeCell(r, 5, mask(c.getIdNumber()), dataStyle);
			writeCell(r, 6, c.getKycStatus().name(),
					statusStyleFor(c.getKycStatus(), statusOk, statusPend, statusFail));
			writeCell(r, 7, safe(c.getCity()), dataStyle);
			writeCell(r, 8, safe(c.getState()), dataStyle);
			writeCell(r, 9, c.getDateOfBirth() != null ? c.getDateOfBirth().toString() : "", dataStyle);
			writeCell(r, 10, formatInstant(c.getCreatedAt()), dataStyle);
		}

		if (customers.isEmpty()) {
			Row r = sheet.createRow(1);
			writeCell(r, 0, "No records found", dataStyle);
		}

		for (int i = 0; i < headers.length; i++) {
			sheet.autoSizeColumn(i);
			sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 800, 12000));
		}
	}

	// ── Excel sheet names can't contain: \ / ? * [ ] : and max 31 chars ──
	private String sanitizeSheetName(String name) {
		String clean = name.replaceAll("[\\\\/?*\\[\\]:]", " ");
		return clean.length() > 31 ? clean.substring(0, 31) : clean;
	}

	// ── Style helpers ───────────────────────────────────────────────

	private CellStyle headerStyle(XSSFWorkbook wb) {
		CellStyle s = wb.createCellStyle();
		XSSFFont f = wb.createFont();
		f.setBold(true);
		f.setColor(IndexedColors.WHITE.getIndex());
		f.setFontHeightInPoints((short) 11);
		s.setFont(f);
		s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
		s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		s.setAlignment(HorizontalAlignment.CENTER);
		s.setBorderBottom(BorderStyle.THIN);
		return s;
	}

	private CellStyle dataStyle(XSSFWorkbook wb) {
		CellStyle s = wb.createCellStyle();
		s.setBorderBottom(BorderStyle.HAIR);
		return s;
	}

	private CellStyle statusStyle(XSSFWorkbook wb, IndexedColors color) {
		CellStyle s = wb.createCellStyle();
		s.setFillForegroundColor(color.getIndex());
		s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		s.setAlignment(HorizontalAlignment.CENTER);
		XSSFFont f = wb.createFont();
		f.setBold(true);
		s.setFont(f);
		return s;
	}

	private CellStyle statusStyleFor(KycStatus status, CellStyle ok, CellStyle pending, CellStyle failed) {
		return switch (status) {
		case VERIFIED -> ok;
		case PENDING -> pending;
		case FAILED -> failed;
		};
	}

	// ── Cell + string helpers ───────────────────────────────────────

	private void writeCell(Row r, int col, Object value, CellStyle style) {
		Cell c = r.createCell(col);
		if (value == null) {
			c.setCellValue("");
		} else if (value instanceof Number n) {
			c.setCellValue(n.doubleValue());
		} else {
			c.setCellValue(String.valueOf(value));
		}
		c.setCellStyle(style);
	}

	private String safe(String s) {
		return s == null ? "" : s;
	}

	private String mask(String v) {
		if (v == null || v.length() < 4)
			return v;
		return "****" + v.substring(v.length() - 4);
	}

	private String formatInstant(Instant instant) {
		if (instant == null)
			return "";
		return LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
				.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss"));
	}
}