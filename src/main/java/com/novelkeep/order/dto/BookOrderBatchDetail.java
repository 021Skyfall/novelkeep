package com.novelkeep.order.dto;

import java.util.List;

public record BookOrderBatchDetail(
        BookOrderBatchRow batch,
        List<BookOrderLineRow> lines
) {
}
