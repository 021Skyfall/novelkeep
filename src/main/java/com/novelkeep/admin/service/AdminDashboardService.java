package com.novelkeep.admin.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.novelkeep.admin.dto.AdminDashboardStats;
import com.novelkeep.funding.domain.FundingCampaignStatus;
import com.novelkeep.funding.domain.FundingPaymentStatus;
import com.novelkeep.funding.repository.FundingCampaignRepository;
import com.novelkeep.funding.repository.FundingParticipationRepository;
import com.novelkeep.novel.domain.EpisodeStatus;
import com.novelkeep.novel.domain.NovelVisibility;
import com.novelkeep.novel.repository.EpisodeRepository;
import com.novelkeep.novel.repository.NovelRepository;
import com.novelkeep.order.domain.BookOrderStatus;
import com.novelkeep.order.dto.BookOrderBatchRow;
import com.novelkeep.order.dto.BookOrderSearchCriteria;
import com.novelkeep.order.repository.BookOrderRepository;
import com.novelkeep.order.service.BookOrderService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {

    private final NovelRepository novelRepository;
    private final EpisodeRepository episodeRepository;
    private final FundingCampaignRepository fundingCampaignRepository;
    private final FundingParticipationRepository fundingParticipationRepository;
    private final BookOrderRepository bookOrderRepository;
    private final BookOrderService bookOrderService;

    public AdminDashboardService(
            NovelRepository novelRepository,
            EpisodeRepository episodeRepository,
            FundingCampaignRepository fundingCampaignRepository,
            FundingParticipationRepository fundingParticipationRepository,
            BookOrderRepository bookOrderRepository,
            BookOrderService bookOrderService
    ) {
        this.novelRepository = novelRepository;
        this.episodeRepository = episodeRepository;
        this.fundingCampaignRepository = fundingCampaignRepository;
        this.fundingParticipationRepository = fundingParticipationRepository;
        this.bookOrderRepository = bookOrderRepository;
        this.bookOrderService = bookOrderService;
    }

    @Transactional(readOnly = true)
    public AdminDashboardStats load() {
        long open = fundingCampaignRepository.countByStatus(FundingCampaignStatus.OPEN);
        long success = fundingCampaignRepository.countByStatus(FundingCampaignStatus.SUCCESS);
        long failed = fundingCampaignRepository.countByStatus(FundingCampaignStatus.FAILED);
        long closed = success + failed;
        int successRate = closed == 0
                ? 0
                : BigDecimal.valueOf(success)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(closed), 0, RoundingMode.HALF_UP)
                .intValue();

        Map<String, Long> orderByStatus = new LinkedHashMap<>();
        for (BookOrderStatus status : BookOrderStatus.values()) {
            orderByStatus.put(status.getDisplayName(), bookOrderRepository.countByStatus(status));
        }

        BookOrderSearchCriteria recentCriteria = new BookOrderSearchCriteria();
        List<BookOrderBatchRow> recent = bookOrderService.searchBatches(recentCriteria).stream()
                .limit(5)
                .toList();

        BigDecimal paid = fundingParticipationRepository.sumMockPaidAmountByPaymentStatus(
                FundingPaymentStatus.PAID_MOCK
        );
        BigDecimal refunded = fundingParticipationRepository.sumMockPaidAmountByPaymentStatus(
                FundingPaymentStatus.REFUNDED_MOCK
        );

        return new AdminDashboardStats(
                novelRepository.count(),
                episodeRepository.countByStatusAndStoryPartNovelVisibility(
                        EpisodeStatus.PUBLISHED,
                        NovelVisibility.PUBLIC
                ),
                open,
                success,
                failed,
                paid == null ? BigDecimal.ZERO : paid,
                refunded == null ? BigDecimal.ZERO : refunded,
                orderByStatus,
                successRate,
                bookOrderRepository.countByStatus(BookOrderStatus.PENDING),
                recent
        );
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv() {
        AdminDashboardStats stats = load();
        StringBuilder sb = new StringBuilder();
        sb.append('\ufeff');
        sb.append("항목,값\n");
        sb.append("전체 작품,").append(stats.novelCount()).append('\n');
        sb.append("공개 회차,").append(stats.publishedEpisodeCount()).append('\n');
        sb.append("펀딩 중,").append(stats.openFundingCount()).append('\n');
        sb.append("성공 펀딩,").append(stats.successFundingCount()).append('\n');
        sb.append("실패 펀딩,").append(stats.failedFundingCount()).append('\n');
        sb.append("펀딩 성공률(%),").append(stats.successRatePercent()).append('\n');
        sb.append("모의 결제 합계,").append(stats.paidAmountSum()).append('\n');
        sb.append("모의 환불 합계,").append(stats.refundedAmountSum()).append('\n');
        sb.append("접수 대기 주문,").append(stats.pendingOrderCount()).append('\n');
        for (Map.Entry<String, Long> entry : stats.orderCountByStatus().entrySet()) {
            sb.append("주문·").append(entry.getKey()).append(',').append(entry.getValue()).append('\n');
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportJson() {
        AdminDashboardStats stats = load();
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"novelCount\": ").append(stats.novelCount()).append(",\n");
        sb.append("  \"publishedEpisodeCount\": ").append(stats.publishedEpisodeCount()).append(",\n");
        sb.append("  \"openFundingCount\": ").append(stats.openFundingCount()).append(",\n");
        sb.append("  \"successFundingCount\": ").append(stats.successFundingCount()).append(",\n");
        sb.append("  \"failedFundingCount\": ").append(stats.failedFundingCount()).append(",\n");
        sb.append("  \"successRatePercent\": ").append(stats.successRatePercent()).append(",\n");
        sb.append("  \"paidAmountSum\": ").append(stats.paidAmountSum()).append(",\n");
        sb.append("  \"refundedAmountSum\": ").append(stats.refundedAmountSum()).append(",\n");
        sb.append("  \"pendingOrderCount\": ").append(stats.pendingOrderCount()).append(",\n");
        sb.append("  \"orderCountByStatus\": {\n");
        int i = 0;
        for (Map.Entry<String, Long> entry : stats.orderCountByStatus().entrySet()) {
            if (i++ > 0) {
                sb.append(",\n");
            }
            sb.append("    ").append(jsonString(entry.getKey())).append(": ").append(entry.getValue());
        }
        sb.append("\n  }\n");
        sb.append("}\n");
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                + "\"";
    }
}
