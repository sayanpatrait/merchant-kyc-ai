package com.isg.kyc.ai;

import com.isg.kyc.model.KycStatus;
import com.isg.kyc.repository.CustomerRepository;
import com.isg.kyc.repository.MerchantRepository;
import com.isg.kyc.service.AnalyticsService;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class ReportTools {

	private final MerchantRepository merchantRepo;
	private final CustomerRepository customerRepo;
	private final AnalyticsService analyticsService;

	public ReportTools(MerchantRepository merchantRepo, CustomerRepository customerRepo, AnalyticsService analyticsService) {
		this.merchantRepo = merchantRepo;
		this.customerRepo = customerRepo;
		this.analyticsService = analyticsService;
	}

	@Tool(description = "Get counts of FAILED and PENDING merchants and customers. Use when the user asks 'how many failed' or 'how many pending' without requesting an Excel file.")
	public String failedPendingSummary() {
		long failedMerchants = merchantRepo.findAll().stream().filter(m -> m.getPanStatus() == KycStatus.FAILED
				|| m.getAadhaarStatus() == KycStatus.FAILED || m.getGstStatus() == KycStatus.FAILED).count();
		long pendingMerchants = merchantRepo.findAll().stream().filter(m -> m.getPanStatus() == KycStatus.PENDING
				|| m.getAadhaarStatus() == KycStatus.PENDING || m.getGstStatus() == KycStatus.PENDING).count();
		long failedCustomers = customerRepo.findAll().stream().filter(c -> c.getKycStatus() == KycStatus.FAILED)
				.count();
		long pendingCustomers = customerRepo.findAll().stream().filter(c -> c.getKycStatus() == KycStatus.PENDING)
				.count();

		return String.format("""
				📊 Failed & Pending Summary
				─────────────────────────────
				Merchants:
				  ❌ Failed:  %d
				  ⏳ Pending: %d
				Customers:
				  ❌ Failed:  %d
				  ⏳ Pending: %d
				""", failedMerchants, pendingMerchants, failedCustomers, pendingCustomers);
	}

	@Tool(description = "Generate an Excel report containing all FAILED and PENDING merchants and customers in separate sheets. Returns a download URL. Use when the user asks for failures, pending items, or 'give me the report of failed/pending merchants and customers'.")
	public String generateFailedPendingReport() {
		return """
				📥 Failed / Pending report ready:
				http://localhost:8080/api/reports/failed-pending.xlsx

				Sheets:
				• ❌ Failed Merchants
				• ⏳ Pending Merchants
				• ❌ Failed Customers
				• ⏳ Pending Customers
				""";
	}

	@Tool(description = "Generate the full KYC report with all data sheets. Returns a download URL.")
	public String generateFullReport() {
		return """
				📥 Full KYC report ready:
				http://localhost:8080/api/reports/kyc.xlsx
				""";
	}
	
	@Tool(description = "Get a heatmap of verification activity by hour of day. Groups into 4 buckets: 00-06, 06-12, 12-18, 18-24. Shows when merchants and customers are most actively onboarded. Use when the user asks 'when do most verifications happen', 'peak hours', 'activity by time', or 'hourly heatmap'.")
	public String getHourlyHeatmap() {
	    return analyticsService.hourlyHeatmapText();
	}
}
