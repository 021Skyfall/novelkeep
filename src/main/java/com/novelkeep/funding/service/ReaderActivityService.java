package com.novelkeep.funding.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.novelkeep.funding.domain.FundingParticipation;
import com.novelkeep.funding.dto.ReaderActivityRow;
import com.novelkeep.funding.dto.ReaderActivitySearchCriteria;
import com.novelkeep.funding.repository.FundingParticipationRepository;
import com.novelkeep.order.domain.BookOrder;
import com.novelkeep.order.domain.BookOrderStatus;
import com.novelkeep.order.repository.BookOrderRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReaderActivityService {

    private final FundingParticipationRepository fundingParticipationRepository;
    private final BookOrderRepository bookOrderRepository;

    public ReaderActivityService(
            FundingParticipationRepository fundingParticipationRepository,
            BookOrderRepository bookOrderRepository
    ) {
        this.fundingParticipationRepository = fundingParticipationRepository;
        this.bookOrderRepository = bookOrderRepository;
    }

    @Transactional(readOnly = true)
    public List<ReaderActivityRow> findActivities(Long memberId, ReaderActivitySearchCriteria criteria) {
        if (memberId == null) {
            return List.of();
        }
        ReaderActivitySearchCriteria.Tab tab = criteria == null
                ? ReaderActivitySearchCriteria.Tab.ALL
                : criteria.getTab();
        String titleKeyword = criteria != null && criteria.getNovelTitle() != null
                ? criteria.getNovelTitle().trim().toLowerCase(Locale.ROOT)
                : "";
        BookOrderStatus orderStatusFilter = criteria != null ? criteria.getOrderStatus() : null;
        ReaderActivitySearchCriteria.ActivityStatus activityStatusFilter =
                criteria != null ? criteria.getActivityStatus() : null;
        ReaderActivitySearchCriteria.SortField sortField = criteria != null ? criteria.getSortField() : null;
        ReaderActivitySearchCriteria.SortDir sortDir = criteria != null ? criteria.getSortDir() : null;

        List<FundingParticipation> participations = fundingParticipationRepository.findDetailByMemberId(memberId);
        Map<Long, BookOrder> orderByParticipationId = new HashMap<>();
        for (BookOrder order : bookOrderRepository.findDetailByMemberId(memberId)) {
            orderByParticipationId.put(order.getParticipation().getId(), order);
        }

        // touch novel parts for multiPart label
        for (FundingParticipation participation : participations) {
            participation.getCampaign().getStoryPart().getNovel().getParts().size();
        }

        List<ReaderActivityRow> rows = new ArrayList<>();
        for (FundingParticipation participation : participations) {
            BookOrder order = orderByParticipationId.get(participation.getId());
            ReaderActivityRow row = ReaderActivityRow.fromParticipation(participation, order);
            if (tab != ReaderActivitySearchCriteria.Tab.ALL && row.tab() != tab) {
                continue;
            }
            if (!titleKeyword.isEmpty()) {
                String title = row.novelTitle() == null ? "" : row.novelTitle().toLowerCase(Locale.ROOT);
                if (!title.contains(titleKeyword)) {
                    continue;
                }
            }
            if (orderStatusFilter != null) {
                if (row.tab() != ReaderActivitySearchCriteria.Tab.ORDER
                        || row.orderStatus() != orderStatusFilter) {
                    continue;
                }
            }
            if (activityStatusFilter == ReaderActivitySearchCriteria.ActivityStatus.PARTICIPATING
                    && row.tab() != ReaderActivitySearchCriteria.Tab.ACTIVE) {
                continue;
            }
            if (activityStatusFilter == ReaderActivitySearchCriteria.ActivityStatus.REFUNDED
                    && row.tab() != ReaderActivitySearchCriteria.Tab.REFUND) {
                continue;
            }
            rows.add(row);
        }

        rows.sort(resolveComparator(sortField, sortDir));
        return rows;
    }

    private Comparator<ReaderActivityRow> resolveComparator(
            ReaderActivitySearchCriteria.SortField sortField,
            ReaderActivitySearchCriteria.SortDir sortDir
    ) {
        boolean asc = sortDir == ReaderActivitySearchCriteria.SortDir.ASC;
        Comparator<ReaderActivityRow> comparator = switch (sortField == null
                ? ReaderActivitySearchCriteria.SortField.RECENT
                : sortField) {
            case END -> Comparator.comparing(
                    ReaderActivityRow::endAt,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case GAUGE -> Comparator.comparingInt(ReaderActivityRow::achievementPercent);
            case RECENT -> Comparator.comparing(
                    this::recentAt,
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
        };
        if (!asc) {
            comparator = comparator.reversed();
        }
        return comparator.thenComparing(ReaderActivityRow::participationId, Comparator.reverseOrder());
    }

    private java.time.LocalDateTime recentAt(ReaderActivityRow row) {
        if (row.tab() == ReaderActivitySearchCriteria.Tab.ORDER) {
            return row.orderedAt();
        }
        if (row.tab() == ReaderActivitySearchCriteria.Tab.REFUND) {
            return row.refundedAt() != null ? row.refundedAt() : row.paidAt();
        }
        return row.paidAt();
    }
}
