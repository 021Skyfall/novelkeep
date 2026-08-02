package com.novelkeep.order.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.novelkeep.common.ExportText;
import com.novelkeep.novel.domain.Novel;
import com.novelkeep.novel.domain.StoryPart;
import com.novelkeep.order.domain.BookOrder;
import com.novelkeep.order.domain.BookOrderStatus;
import com.novelkeep.order.dto.BookOrderBatchDetail;
import com.novelkeep.order.dto.BookOrderBatchRow;
import com.novelkeep.order.dto.BookOrderLineRow;
import com.novelkeep.order.dto.BookOrderSearchCriteria;
import com.novelkeep.order.repository.BookOrderRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookOrderService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final BookOrderRepository bookOrderRepository;

    public BookOrderService(BookOrderRepository bookOrderRepository) {
        this.bookOrderRepository = bookOrderRepository;
    }

    @Transactional(readOnly = true)
    public List<BookOrderBatchRow> searchBatches(BookOrderSearchCriteria criteria) {
        List<BookOrder> orders = loadOrders(criteria, false);
        List<BookOrderBatchRow> batches = aggregateByCampaign(orders);
        BookOrderStatus statusFilter = criteria.getStatus();
        if (statusFilter != null) {
            batches = batches.stream()
                    .filter(batch -> batch.status() == statusFilter)
                    .toList();
        }
        return sortBatches(batches, criteria.getSortField(), criteria.getSortDir());
    }

    @Transactional(readOnly = true)
    public BookOrderBatchDetail findBatchDetail(Long campaignId) {
        List<BookOrder> orders = bookOrderRepository.findDetailByCampaignId(campaignId);
        if (orders.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "주문 묶음이 없습니다.");
        }
        // touch novel parts for multiPart label
        orders.get(0).getParticipation().getCampaign().getStoryPart().getNovel().getParts().size();
        BookOrderBatchRow batch = toBatchRow(orders);
        List<BookOrderLineRow> lines = orders.stream().map(BookOrderLineRow::from).toList();
        return new BookOrderBatchDetail(batch, lines);
    }

    @Transactional
    public void advanceCampaignStatus(Long campaignId) {
        List<BookOrder> orders = bookOrderRepository.findDetailByCampaignId(campaignId);
        if (orders.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "주문 묶음이 없습니다.");
        }
        BookOrderStatus least = leastStatus(orders);
        if (least == null || least.next() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 배송 완료된 주문입니다.");
        }
        try {
            for (BookOrder order : orders) {
                if (order.getStatus() == least) {
                    order.advanceStatus();
                }
            }
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(BookOrderSearchCriteria criteria) {
        StringBuilder sb = new StringBuilder();
        sb.append('\ufeff');
        sb.append("주문번호,작품명,권,총수량,참여건수,제품상태,주문일시\n");
        for (BookOrderBatchRow row : searchBatches(criteria)) {
            sb.append(row.minOrderId()).append(',')
                    .append(ExportText.csv(row.novelTitle())).append(',')
                    .append(ExportText.csv(row.partLabel())).append(',')
                    .append(row.totalQuantity()).append(',')
                    .append(row.orderCount()).append(',')
                    .append(ExportText.csv(row.statusLabel())).append(',')
                    .append(ExportText.csv(DATE_TIME.format(row.orderedAt())))
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportJson(BookOrderSearchCriteria criteria) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        List<BookOrderBatchRow> rows = searchBatches(criteria);
        for (int i = 0; i < rows.size(); i++) {
            BookOrderBatchRow row = rows.get(i);
            if (i > 0) {
                sb.append(",\n");
            }
            sb.append("  {\n")
                    .append("    \"orderId\": ").append(row.minOrderId()).append(",\n")
                    .append("    \"campaignId\": ").append(row.campaignId()).append(",\n")
                    .append("    \"novelTitle\": ").append(ExportText.jsonString(row.novelTitle())).append(",\n")
                    .append("    \"partLabel\": ").append(ExportText.jsonString(row.partLabel())).append(",\n")
                    .append("    \"totalQuantity\": ").append(row.totalQuantity()).append(",\n")
                    .append("    \"orderCount\": ").append(row.orderCount()).append(",\n")
                    .append("    \"status\": ").append(ExportText.jsonString(row.status().name())).append(",\n")
                    .append("    \"statusLabel\": ").append(ExportText.jsonString(row.statusLabel())).append(",\n")
                    .append("    \"orderedAt\": ").append(ExportText.jsonString(ISO.format(row.orderedAt()))).append("\n")
                    .append("  }");
        }
        sb.append("\n]\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private List<BookOrder> loadOrders(BookOrderSearchCriteria criteria, boolean applyStatusFilter) {
        LocalDateTime fromAt = criteria.getOrderedFrom() == null
                ? null
                : criteria.getOrderedFrom().atStartOfDay();
        LocalDateTime toAt = criteria.getOrderedTo() == null
                ? null
                : criteria.getOrderedTo().plusDays(1).atStartOfDay();
        return bookOrderRepository.search(
                criteria.normalizedTitle(),
                applyStatusFilter ? criteria.getStatus() : null,
                fromAt,
                toAt
        );
    }

    private List<BookOrderBatchRow> aggregateByCampaign(List<BookOrder> orders) {
        Map<Long, List<BookOrder>> byCampaign = new LinkedHashMap<>();
        for (BookOrder order : orders) {
            Long campaignId = order.getParticipation().getCampaign().getId();
            byCampaign.computeIfAbsent(campaignId, ignored -> new ArrayList<>()).add(order);
        }
        List<BookOrderBatchRow> rows = new ArrayList<>(byCampaign.size());
        for (List<BookOrder> group : byCampaign.values()) {
            group.get(0).getParticipation().getCampaign().getStoryPart().getNovel().getParts().size();
            rows.add(toBatchRow(group));
        }
        return rows;
    }

    private BookOrderBatchRow toBatchRow(List<BookOrder> orders) {
        BookOrder first = orders.get(0);
        var campaign = first.getParticipation().getCampaign();
        StoryPart part = campaign.getStoryPart();
        Novel novel = part.getNovel();
        String partLabel = novel.isMultiPart()
                ? part.getPartNumber() + "부 · " + part.getTitle()
                : "본편";

        int totalQuantity = 0;
        Long minOrderId = null;
        LocalDateTime earliestOrderedAt = null;
        for (BookOrder order : orders) {
            totalQuantity += order.getQuantity();
            if (minOrderId == null || order.getId() < minOrderId) {
                minOrderId = order.getId();
            }
            if (earliestOrderedAt == null || order.getOrderedAt().isBefore(earliestOrderedAt)) {
                earliestOrderedAt = order.getOrderedAt();
            }
        }

        BookOrderStatus status = leastStatus(orders);
        BookOrderStatus next = status == null ? null : status.next();
        return new BookOrderBatchRow(
                campaign.getId(),
                novel.getId(),
                minOrderId,
                novel.getTitle(),
                partLabel,
                totalQuantity,
                orders.size(),
                status,
                status == null ? "-" : status.getDisplayName(),
                earliestOrderedAt,
                next != null,
                next == null ? null : next.getDisplayName()
        );
    }

    private BookOrderStatus leastStatus(List<BookOrder> orders) {
        BookOrderStatus least = null;
        for (BookOrder order : orders) {
            if (least == null || order.getStatus().ordinal() < least.ordinal()) {
                least = order.getStatus();
            }
        }
        return least;
    }

    private List<BookOrderBatchRow> sortBatches(
            List<BookOrderBatchRow> batches,
            BookOrderSearchCriteria.SortField sortField,
            BookOrderSearchCriteria.SortDir sortDir
    ) {
        Comparator<BookOrderBatchRow> byOrderedDesc = Comparator.comparing(
                row -> toMinute(row.orderedAt()),
                Comparator.nullsLast(Comparator.reverseOrder())
        );
        if (sortField == null || sortDir == null) {
            return batches.stream().sorted(byOrderedDesc).toList();
        }
        Comparator<BookOrderBatchRow> byField = switch (sortField) {
            case ID -> Comparator.comparing(BookOrderBatchRow::minOrderId, Comparator.nullsLast(Comparator.naturalOrder()));
            case ORDERED -> Comparator.comparing(
                    row -> toMinute(row.orderedAt()),
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case STATUS -> Comparator.comparingInt(row -> statusRank(row.status()));
        };
        if (sortDir == BookOrderSearchCriteria.SortDir.DESC) {
            byField = byField.reversed();
        }
        return batches.stream().sorted(byField.thenComparing(byOrderedDesc)).toList();
    }

    private static LocalDateTime toMinute(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.withSecond(0).withNano(0);
    }

    private int statusRank(BookOrderStatus status) {
        if (status == null) {
            return 99;
        }
        BookOrderStatus[] values = BookOrderStatus.values();
        for (int i = 0; i < values.length; i++) {
            if (values[i] == status) {
                return i;
            }
        }
        return 99;
    }

}
