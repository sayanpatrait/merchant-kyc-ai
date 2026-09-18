package com.isg.kyc.service;

import com.isg.kyc.model.Customer;
import com.isg.kyc.model.KycStatus;
import com.isg.kyc.model.Merchant;
import com.isg.kyc.repository.CustomerRepository;
import com.isg.kyc.repository.MerchantRepository;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AnalyticsService {

	private final MerchantRepository merchantRepo;
	private final CustomerRepository customerRepo;

	public AnalyticsService(MerchantRepository merchantRepo, CustomerRepository customerRepo) {
		this.merchantRepo = merchantRepo;
		this.customerRepo = customerRepo;
	}

	/**
	 * Returns activity counts per 6-hour bucket, split by entity type. buckets:
	 * 00-06, 06-12, 12-18, 18-24
	 */
	public Map<String, Object> hourlyHeatmap() {
		int[] merchantBuckets = new int[4];
		int[] customerBuckets = new int[4];

		ZoneId zone = ZoneId.systemDefault();

		for (Merchant m : merchantRepo.findAll()) {
			if (m.getCreatedAt() == null)
				continue;
			int hour = ZonedDateTime.ofInstant(m.getCreatedAt(), zone).getHour();
			merchantBuckets[hour / 6]++;
		}

		for (Customer c : customerRepo.findAll()) {
			if (c.getCreatedAt() == null)
				continue;
			int hour = ZonedDateTime.ofInstant(c.getCreatedAt(), zone).getHour();
			customerBuckets[hour / 6]++;
		}

		String[] labels = { "00:00–06:00", "06:00–12:00", "12:00–18:00", "18:00–24:00" };

		int total = 0;
		for (int i = 0; i < 4; i++)
			total += merchantBuckets[i] + customerBuckets[i];

		// Build bucket list
		var buckets = new java.util.ArrayList<Map<String, Object>>();
		for (int i = 0; i < 4; i++) {
			int m = merchantBuckets[i];
			int c = customerBuckets[i];
			int sum = m + c;
			double pct = total == 0 ? 0 : (sum * 100.0 / total);

			Map<String, Object> bucket = new LinkedHashMap<>();
			bucket.put("label", labels[i]);
			bucket.put("merchants", m);
			bucket.put("customers", c);
			bucket.put("total", sum);
			bucket.put("percentage", Math.round(pct * 10.0) / 10.0);
			buckets.add(bucket);
		}

		// Peak
		int peakIdx = 0;
		int peakVal = 0;
		for (int i = 0; i < 4; i++) {
			int sum = merchantBuckets[i] + customerBuckets[i];
			if (sum > peakVal) {
				peakVal = sum;
				peakIdx = i;
			}
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total", total);
		result.put("buckets", buckets);
		result.put("peakBucket", labels[peakIdx]);
		result.put("peakCount", peakVal);

		return result;
	}

	/**
	 * Human-readable text version for the AI to speak.
	 */
	public String hourlyHeatmapText() {
		Map<String, Object> data = hourlyHeatmap();
		long total = ((Number) data.get("total")).longValue();

		if (total == 0) {
			return "📈 No activity recorded yet.";
		}

		@SuppressWarnings("unchecked")
		var buckets = (java.util.List<Map<String, Object>>) data.get("buckets");

		StringBuilder sb = new StringBuilder();
		sb.append("📈 VERIFICATION ACTIVITY BY HOUR\n");
		sb.append("─────────────────────────────────\n");

		for (Map<String, Object> b : buckets) {
			long sum = ((Number) b.get("total")).longValue();
			double pct = ((Number) b.get("percentage")).doubleValue();

			// Build a bar of up to 20 chars
			int barLen = (int) Math.round(pct / 5.0); // 100% → 20 chars
			String bar = "█".repeat(Math.max(0, barLen)) + "░".repeat(Math.max(0, 20 - barLen));

			sb.append(String.format("%s  %s  %4.1f%%  (%d)%n", b.get("label"), bar, pct, sum));
		}

		sb.append("─────────────────────────────────\n");
		sb.append("Total: ").append(total).append(" records\n");
		sb.append("Peak:  ").append(data.get("peakBucket")).append(" with ").append(data.get("peakCount"))
				.append(" records\n");

		return sb.toString();
	}

	/**
	 * Failure reason breakdown across merchants + customers.
	 */
	public Map<String, Object> failureReasons() {
		Map<String, Long> counts = new java.util.HashMap<>();

		for (Merchant m : merchantRepo.findAll()) {
			if (m.getFailureReason() != null && !m.getFailureReason().isBlank()) {
				counts.merge(m.getFailureReason(), 1L, Long::sum);
			}
		}
		for (Customer c : customerRepo.findAll()) {
			if (c.getFailureReason() != null && !c.getFailureReason().isBlank()) {
				counts.merge(c.getFailureReason(), 1L, Long::sum);
			}
		}

		long total = counts.values().stream().mapToLong(Long::longValue).sum();

		// Sort descending
		var sorted = counts.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).toList();

		var items = new java.util.ArrayList<Map<String, Object>>();
		for (var e : sorted) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("reason", e.getKey());
			item.put("count", e.getValue());
			item.put("percentage", total == 0 ? 0.0 : Math.round((e.getValue() * 10000.0 / total)) / 100.0);
			items.add(item);
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("total", total);
		result.put("reasons", items);
		return result;
	}

	/**
	 * Overall success rate across all checks.
	 */
	public Map<String, Object> successRate() {
		long verified = 0;
		long failed = 0;
		long pending = 0;

		for (Merchant m : merchantRepo.findAll()) {
			verified += (m.getPanStatus() == KycStatus.VERIFIED ? 1 : 0);
			verified += (m.getAadhaarStatus() == KycStatus.VERIFIED ? 1 : 0);
			verified += (m.getGstStatus() == KycStatus.VERIFIED ? 1 : 0);
			failed += (m.getPanStatus() == KycStatus.FAILED ? 1 : 0);
			failed += (m.getAadhaarStatus() == KycStatus.FAILED ? 1 : 0);
			failed += (m.getGstStatus() == KycStatus.FAILED ? 1 : 0);
			pending += (m.getPanStatus() == KycStatus.PENDING ? 1 : 0);
			pending += (m.getAadhaarStatus() == KycStatus.PENDING ? 1 : 0);
			pending += (m.getGstStatus() == KycStatus.PENDING ? 1 : 0);
		}

		for (Customer c : customerRepo.findAll()) {
			verified += (c.getKycStatus() == KycStatus.VERIFIED ? 1 : 0);
			failed += (c.getKycStatus() == KycStatus.FAILED ? 1 : 0);
			pending += (c.getKycStatus() == KycStatus.PENDING ? 1 : 0);
		}

		long total = verified + failed;
		double rate = total == 0 ? 0.0 : Math.round((verified * 10000.0 / total)) / 100.0;

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("verified", verified);
		result.put("failed", failed);
		result.put("pending", pending);
		result.put("total", total);
		result.put("rate", rate);
		return result;
	}
}