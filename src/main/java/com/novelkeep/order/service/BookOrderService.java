package com.novelkeep.order.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import com.novelkeep.order.domain.BookOrder;
import com.novelkeep.order.domain.BookOrderStatus;
import com.novelkeep.order.dto.BookOrderRow;
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
    public List<BookOrderRow> search(BookOrderSearchCriteria criteria) {
        return load(criteria).stream().map(BookOrderRow::from).toList();
    }

    @Transactional
    public void advanceStatus(Long orderId) {
        BookOrder order = bookOrderRepository.findDetailById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            order.advanceStatus();
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(BookOrderSearchCriteria criteria) {
        StringBuilder sb = new StringBuilder();
        sb.append('\ufeff');
        sb.append("주문번호,작품명,권,수량,상태,주문일시\n");
        for (BookOrderRow row : search(criteria)) {
            sb.append(row.id()).append(',')
                    .append(csv(row.novelTitle())).append(',')
                    .append(csv(row.partLabel())).append(',')
                    .append(row.quantity()).append(',')
                    .append(csv(row.statusLabel())).append(',')
                    .append(csv(DATE_TIME.format(row.orderedAt())))
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportJson(BookOrderSearchCriteria criteria) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        List<BookOrderRow> rows = search(criteria);
        for (int i = 0; i < rows.size(); i++) {
            BookOrderRow row = rows.get(i);
            if (i > 0) {
                sb.append(",\n");
            }
            sb.append("  {\n")
                    .append("    \"id\": ").append(row.id()).append(",\n")
                    .append("    \"novelTitle\": ").append(jsonString(row.novelTitle())).append(",\n")
                    .append("    \"partLabel\": ").append(jsonString(row.partLabel())).append(",\n")
                    .append("    \"quantity\": ").append(row.quantity()).append(",\n")
                    .append("    \"status\": ").append(jsonString(row.status().name())).append(",\n")
                    .append("    \"statusLabel\": ").append(jsonString(row.statusLabel())).append(",\n")
                    .append("    \"orderedAt\": ").append(jsonString(ISO.format(row.orderedAt()))).append("\n")
                    .append("  }");
        }
        sb.append("\n]\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private List<BookOrder> load(BookOrderSearchCriteria criteria) {
        LocalDateTime fromAt = criteria.getOrderedFrom() == null
                ? null
                : criteria.getOrderedFrom().atStartOfDay();
        LocalDateTime toAt = criteria.getOrderedTo() == null
                ? null
                : criteria.getOrderedTo().plusDays(1).atStartOfDay();
        List<BookOrder> orders = bookOrderRepository.search(
                criteria.normalizedTitle(),
                criteria.getStatus(),
                fromAt,
                toAt
        );
        orders.sort(resolveOrderComparator(criteria.getSortField(), criteria.getSortDir()));
        return orders;
    }

    private Comparator<BookOrder> resolveOrderComparator(
            BookOrderSearchCriteria.SortField sortField,
            BookOrderSearchCriteria.SortDir sortDir
    ) {
        Comparator<BookOrder> byOrderedDesc = Comparator.comparing(
                o -> toMinute(o.getOrderedAt()),
                Comparator.nullsLast(Comparator.reverseOrder())
        );
        if (sortField == null || sortDir == null) {
            return byOrderedDesc;
        }
        Comparator<BookOrder> byField = switch (sortField) {
            case ID -> Comparator.comparing(BookOrder::getId);
            case ORDERED -> Comparator.comparing(
                    o -> toMinute(o.getOrderedAt()),
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case STATUS -> Comparator.comparingInt(order -> statusRank(order.getStatus()));
        };
        if (sortDir == BookOrderSearchCriteria.SortDir.DESC) {
            byField = byField.reversed();
        }
        return byField.thenComparing(byOrderedDesc);
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

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }
}
