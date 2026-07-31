package com.novelkeep.funding.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public final class FundingGuide {

    public static final int MIN_TARGET_QUANTITY = 10;
    public static final int MIN_DURATION_DAYS = 7;
    public static final int GUIDE_VOLUME_CHARS = 100_000;
    public static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    public static final String GUIDE_VOLUME_TEXT =
            "일반 소설책 1권은 대략 8~10만 자 정도입니다. 분량은 작가 재량입니다. 공개 회차 합이 10만 자를 넘으면 출판 담당과 문의가 필요합니다.";

    private FundingGuide() {
    }

    public static LocalDateTime nowKorea() {
        return LocalDateTime.now(KOREA_ZONE);
    }

    public static List<String> startNotices() {
        return List.of(
                "목표 부수는 최소 " + MIN_TARGET_QUANTITY + "부입니다.",
                "부마다 진행 중 펀딩은 1개만 둘 수 있습니다. 여러 부는 동시에 진행할 수 있습니다.",
                "미공개 부, 회차 없음, 미공개 회차가 있으면 시작할 수 없습니다.",
                GUIDE_VOLUME_TEXT,
                "성공 조건: 해당 부 완결 + 목표 부수 달성.",
                "시작 후 시작일은 바꿀 수 없습니다. 목표 부수·판매가·종료일만 수정할 수 있습니다.",
                "참여(수요)가 0이면 직접 취소할 수 있습니다. 1부 이상이면 담당자에게 문의해 주세요.",
                "진행 중에는 같은 부로 다시 시작할 수 없습니다. 종료된 뒤에는 다시 시작할 수 있습니다.",
                "종료일은 시작일 이후여야 하고, 시작일로부터 최소 " + MIN_DURATION_DAYS
                        + "일 이상이며, 현재 시간보다 이후여야 합니다."
        );
    }

    public static void validateCreateSchedule(LocalDateTime startAt, LocalDateTime endAt) {
        LocalDateTime now = nowKorea();
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException("시작일과 종료일을 입력해 주세요.");
        }
        if (startAt.isBefore(now.minusMinutes(1))) {
            throw new IllegalArgumentException("시작일은 현재 시간 이전일 수 없습니다.");
        }
        validateEndSchedule(startAt, endAt, now);
    }

    public static void validateEndSchedule(LocalDateTime startAt, LocalDateTime endAt, LocalDateTime now) {
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException("시작일과 종료일을 입력해 주세요.");
        }
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("종료일은 시작일보다 이후여야 합니다.");
        }
        if (endAt.isBefore(startAt.plusDays(MIN_DURATION_DAYS))) {
            throw new IllegalArgumentException(
                    "종료일은 시작일로부터 최소 " + MIN_DURATION_DAYS + "일 이후여야 합니다."
            );
        }
        if (endAt.isBefore(now)) {
            throw new IllegalArgumentException("종료일은 현재 시간 이전일 수 없습니다.");
        }
    }

    public static void validateTarget(int targetQuantity, BigDecimal priceAmount) {
        if (targetQuantity < MIN_TARGET_QUANTITY) {
            throw new IllegalArgumentException("목표 부수는 " + MIN_TARGET_QUANTITY + "부 이상이어야 합니다.");
        }
        if (priceAmount == null || priceAmount.compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException("판매가는 1 이상이어야 합니다.");
        }
    }
}
