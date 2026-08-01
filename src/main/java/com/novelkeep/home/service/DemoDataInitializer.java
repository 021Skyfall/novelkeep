package com.novelkeep.home.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.novelkeep.funding.domain.FundingCampaign;
import com.novelkeep.funding.domain.FundingGuide;
import com.novelkeep.funding.domain.FundingParticipation;
import com.novelkeep.funding.repository.FundingCampaignRepository;
import com.novelkeep.funding.repository.FundingParticipationRepository;
import com.novelkeep.member.domain.Member;
import com.novelkeep.member.domain.MemberType;
import com.novelkeep.member.repository.MemberRepository;
import com.novelkeep.novel.domain.Episode;
import com.novelkeep.novel.domain.EpisodeStatus;
import com.novelkeep.novel.domain.Novel;
import com.novelkeep.novel.domain.NovelGenre;
import com.novelkeep.novel.domain.NovelStatus;
import com.novelkeep.novel.domain.NovelVisibility;
import com.novelkeep.novel.domain.StoryPart;
import com.novelkeep.novel.domain.StoryPartStatus;
import com.novelkeep.novel.repository.NovelRepository;
import com.novelkeep.order.domain.BookOrder;
import com.novelkeep.order.domain.BookOrderStatus;
import com.novelkeep.order.repository.BookOrderRepository;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 체험용 목 데이터.
 * <p>
 * 펀딩 정책 반영: 공개 작품·공개 부·전 회차 공개인 부만 OPEN 가능,
 * 부마다 별도 캠페인(동시 진행 가능), 시작 후 취소 없음, 목표 최소 10부,
 * 기간은 시작 후 최소 7일, 분량 안내(약 8~10만 자 / 10만 초과 시 문의).
 */
@Component
public class DemoDataInitializer implements ApplicationRunner {

    private static final int NOVEL_COUNT = 50;
    private static final int ORDER_SEED_COUNT = 12;
    private static final long RANDOM_SEED = 20260801L;

    private static final String[] TITLE_PREFIXES = {
            "달빛 아래", "마지막", "잊힌", "회귀한", "심해의",
            "황궁의", "새벽을 걷는", "별을 품은", "겨울 끝", "검은"
    };
    private static final String[] TITLE_SUBJECTS = {
            "서기관", "우체국", "정원사", "검술 교관", "약사",
            "도서관", "배달부", "왕녀", "탐험가", "시간 상점",
            "마법학교", "기억 수집가"
    };
    private static final String[] PEN_NAMES = {
            "푸른잉크", "새벽편지", "작은궤도", "서리칼날", "야간점주",
            "먹빛새", "해질녘", "은빛나침반", "참치구름", "검은문장"
    };
    private static final String[] STORY_KEYWORDS = {
            "사라진 기록", "봉인된 기억", "두 번째 기회", "비밀 계약",
            "멸망의 예언", "돌아오지 않는 편지", "끝나지 않은 약속", "낯선 신호"
    };
    private static final String[] PART_TITLES = {
            "시작의 문", "낯선 동행", "무너진 경계", "되돌아온 약속",
            "감춰진 이름", "마지막 선택", "새벽의 기록", "별이 지는 곳"
    };
    private static final String[] EPISODE_EVENTS = {
            "예상하지 못한 손님이 찾아왔다", "오래된 문서에서 단서를 발견했다",
            "닫혀 있던 문이 다시 열렸다", "믿었던 동료가 비밀을 고백했다",
            "도시 전체에 낯선 신호가 울렸다", "과거의 약속이 현재를 흔들었다",
            "사라진 물건이 뜻밖의 장소에서 발견됐다", "새로운 길을 선택해야 했다",
            "감춰진 이름의 주인이 모습을 드러냈다", "평온했던 일상이 한순간에 무너졌다"
    };

    private final MemberRepository memberRepository;
    private final NovelRepository novelRepository;
    private final FundingCampaignRepository fundingCampaignRepository;
    private final FundingParticipationRepository fundingParticipationRepository;
    private final BookOrderRepository bookOrderRepository;

    public DemoDataInitializer(
            MemberRepository memberRepository,
            NovelRepository novelRepository,
            FundingCampaignRepository fundingCampaignRepository,
            FundingParticipationRepository fundingParticipationRepository,
            BookOrderRepository bookOrderRepository
    ) {
        this.memberRepository = memberRepository;
        this.novelRepository = novelRepository;
        this.fundingCampaignRepository = fundingCampaignRepository;
        this.fundingParticipationRepository = fundingParticipationRepository;
        this.bookOrderRepository = bookOrderRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (memberRepository.count() > 0 || novelRepository.count() > 0) {
            return;
        }

        Member reader = memberRepository.save(Member.create(MemberType.READER));
        Member author = memberRepository.save(Member.create(MemberType.AUTHOR));
        memberRepository.save(Member.create(MemberType.ADMIN));

        List<Novel> novels = createNovels(author);
        novelRepository.saveAll(novels);
        seedNovelBrowseStats(novels);
        List<FundingCampaign> campaigns = fundingCampaignRepository.saveAll(createFundings(novels));
        seedParticipationsAndOrders(reader, campaigns);
        seedCampaignUpdatedAt(fundingCampaignRepository.findAll());
    }

    private void seedNovelBrowseStats(List<Novel> novels) {
        LocalDateTime now = LocalDateTime.now();
        Random random = new Random(RANDOM_SEED + 31);
        for (Novel novel : novels) {
            long recommendations = random.nextInt(180);
            // 탐색 카드 9999+ 표기·게이지 길이 확인용
            if (novel.getId() != null && novel.getId() % 17 == 0) {
                recommendations = 10_000L + random.nextInt(2_500);
            } else if (novel.getId() != null && novel.getId() % 11 == 0) {
                recommendations = 1_000L + random.nextInt(8_000);
            }
            LocalDateTime updatedAt = now
                    .minusDays(random.nextInt(60))
                    .minusHours(random.nextInt(24))
                    .minusMinutes(random.nextInt(60));
            novelRepository.seedDemoStats(novel.getId(), recommendations, updatedAt);
        }
    }

    private void seedCampaignUpdatedAt(List<FundingCampaign> campaigns) {
        LocalDateTime now = LocalDateTime.now();
        Random random = new Random(RANDOM_SEED + 47);
        for (FundingCampaign campaign : campaigns) {
            LocalDateTime updatedAt = now
                    .minusDays(random.nextInt(90))
                    .minusHours(random.nextInt(24))
                    .minusMinutes(random.nextInt(60))
                    .minusSeconds(random.nextInt(60));
            fundingCampaignRepository.seedDemoUpdatedAt(campaign.getId(), updatedAt);
        }
    }

    /**
     * 게이지(currentQuantity)와 실제 참여 행을 맞춘다.
     * 독자 회원은 1명이므로 캠페인당 참여 1건·quantity = 수요 부로 맞춘다.
     * 일부는 성공 가능(완결부 + 목표 달성), 일부는 승인 대기·승인 완료 시나리오를 만든다.
     */
    private void seedParticipationsAndOrders(Member reader, List<FundingCampaign> campaigns) {
        LocalDateTime now = LocalDateTime.now();
        Random random = new Random(RANDOM_SEED + 17);
        BookOrderStatus[] statuses = BookOrderStatus.values();
        List<FundingParticipation> participations = new ArrayList<>();
        List<BookOrder> orders = new ArrayList<>();

        List<FundingCampaign> openCampaigns = campaigns.stream()
                .filter(campaign -> campaign.getStatus() == com.novelkeep.funding.domain.FundingCampaignStatus.OPEN)
                .toList();

        int orderSeedIndex = 0;
        int awaitingSuccessSeed = 0;
        int awaitingFailSeed = 0;
        int reservedOpenSuccess = 0;

        for (int i = 0; i < openCampaigns.size(); i++) {
            FundingCampaign campaign = openCampaigns.get(i);
            int demand = campaign.getCurrentQuantity();
            if (demand <= 0) {
                campaign.syncCurrentQuantity(0);
                continue;
            }

            LocalDateTime paidAt = now
                    .minusDays(random.nextInt(40))
                    .minusHours(random.nextInt(24))
                    .minusMinutes(random.nextInt(60));
            FundingParticipation participation = FundingParticipation.paid(
                    campaign,
                    reader,
                    demand,
                    paidAt
            );
            participations.add(participation);
            campaign.syncCurrentQuantity(demand);

            if (campaign.isSuccessReady()) {
                // 출판 상태 6단계 필터·문구 확인용: 먼저 전 단계 1건씩 확보
                if (orderSeedIndex < statuses.length) {
                    campaign.closeAsSuccess();
                    campaign.markApproved(now
                            .minusDays(random.nextInt(12))
                            .minusHours(random.nextInt(24))
                            .minusMinutes(random.nextInt(60)));
                    orders.add(BookOrder.fromParticipation(
                            participation,
                            statuses[orderSeedIndex],
                            now.minusDays(random.nextInt(20))
                                    .minusHours(random.nextInt(24))
                                    .minusMinutes(random.nextInt(60))
                    ));
                    orderSeedIndex++;
                    continue;
                }
                // 작가 성공 마감 테스트용 OPEN을 확보
                if (reservedOpenSuccess < 3) {
                    reservedOpenSuccess++;
                    continue;
                }
                // 승인 대기 성공 샘플
                if (awaitingSuccessSeed < 2) {
                    campaign.closeAsSuccess();
                    awaitingSuccessSeed++;
                    continue;
                }
                // 추가 승인 완료 주문
                if (orderSeedIndex < ORDER_SEED_COUNT) {
                    campaign.closeAsSuccess();
                    campaign.markApproved(now
                            .minusDays(random.nextInt(12))
                            .minusHours(random.nextInt(24))
                            .minusMinutes(random.nextInt(60)));
                    orders.add(BookOrder.fromParticipation(
                            participation,
                            statuses[orderSeedIndex % statuses.length],
                            now.minusDays(random.nextInt(20))
                                    .minusHours(random.nextInt(24))
                                    .minusMinutes(random.nextInt(60))
                    ));
                    orderSeedIndex++;
                }
                continue;
            }

            // 승인 대기 실패 샘플
            if (awaitingFailSeed < 2) {
                campaign.closeAsFailed();
                awaitingFailSeed++;
            }
        }

        // 성공 가능 OPEN이 하나도 없으면, 참여 없는 완결부 캠페인에 목표 달성 수요를 붙인다.
        ensureSuccessReadyOpen(openCampaigns, reader, participations, now);

        fundingParticipationRepository.saveAll(participations);
        if (!orders.isEmpty()) {
            bookOrderRepository.saveAll(orders);
        }
        fundingCampaignRepository.saveAll(campaigns);
    }

    private void ensureSuccessReadyOpen(
            List<FundingCampaign> openCampaigns,
            Member reader,
            List<FundingParticipation> participations,
            LocalDateTime now
    ) {
        boolean hasReadyOpen = openCampaigns.stream()
                .anyMatch(c -> c.getStatus() == com.novelkeep.funding.domain.FundingCampaignStatus.OPEN
                        && c.isSuccessReady());
        if (hasReadyOpen) {
            return;
        }

        for (FundingCampaign campaign : openCampaigns) {
            if (campaign.getStatus() != com.novelkeep.funding.domain.FundingCampaignStatus.OPEN) {
                continue;
            }
            if (campaign.getStoryPart().getStatus() != StoryPartStatus.COMPLETED) {
                continue;
            }
            boolean alreadyParticipated = participations.stream()
                    .anyMatch(p -> p.getCampaign() == campaign);
            if (alreadyParticipated) {
                continue;
            }
            int target = campaign.getTargetQuantity();
            campaign.syncCurrentQuantity(target);
            participations.add(FundingParticipation.paid(campaign, reader, target, now.minusHours(1)));
            return;
        }
    }

    private List<Novel> createNovels(Member author) {
        Random metadataRandom = new Random(RANDOM_SEED);
        Random structureRandom = new Random(RANDOM_SEED + 1);
        List<Novel> novels = new ArrayList<>();

        for (int number = 1; number <= NOVEL_COUNT; number++) {
            NovelProfile profile = resolveProfile(number);
            List<NovelGenre> genres = pickGenres(number);
            String title = TITLE_PREFIXES[metadataRandom.nextInt(TITLE_PREFIXES.length)]
                    + " " + TITLE_SUBJECTS[metadataRandom.nextInt(TITLE_SUBJECTS.length)]
                    + " " + String.format("%02d", number);
            String penName = PEN_NAMES[metadataRandom.nextInt(PEN_NAMES.length)];
            String synopsis = genres.getFirst().getDisplayName() + " 세계에서 "
                    + STORY_KEYWORDS[number % STORY_KEYWORDS.length]
                    + "을 추적하는 인물들의 선택과 성장을 그린 이야기.";

            Novel novel = Novel.create(
                    author, title, penName, genres, synopsis,
                    profile.status(), profile.visibility()
            );
            buildParts(novel, number, profile, structureRandom);
            novels.add(novel);
        }
        return novels;
    }

    /**
     * 시나리오 분포 (50작품 기준 대략치)
     * - PRIVATE: 미공개 작품(펀딩 OPEN 없음)
     * - COMPLETED 다부: 완결·전 회차 공개 → 펀딩 후보
     * - SERIALIZING 다부: 1부 완결(펀딩 가능) + 연재부(미공개 회차 섞임, 펀딩 불가) + 미공개부
     * - SERIALIZING 단권: 일부는 전 회차 공개(펀딩 가능), 일부는 미공개 회차 있음(펀딩 불가)
     */
    private NovelProfile resolveProfile(int number) {
        if (number % 8 == 0) {
            return new NovelProfile(NovelStatus.SERIALIZING, NovelVisibility.PRIVATE, true, false);
        }
        if (number % 5 == 0) {
            return new NovelProfile(NovelStatus.COMPLETED, NovelVisibility.PUBLIC, true, true);
        }
        if (number % 3 == 0) {
            return new NovelProfile(NovelStatus.SERIALIZING, NovelVisibility.PUBLIC, true, false);
        }
        boolean single = number % 4 == 1;
        boolean allPublished = number % 2 == 0;
        return new NovelProfile(
                NovelStatus.SERIALIZING,
                NovelVisibility.PUBLIC,
                !single,
                allPublished
        );
    }

    private void buildParts(Novel novel, int novelNumber, NovelProfile profile, Random random) {
        // 내 작품 리스트 '외 N건' 확인용: 5부 전부 펀딩 가능
        if (novelNumber == 20) {
            for (int partNumber = 1; partNumber <= 5; partNumber++) {
                String partTitle = PART_TITLES[(novelNumber + partNumber) % PART_TITLES.length];
                StoryPart part = StoryPart.create(partNumber, partTitle, StoryPartStatus.COMPLETED);
                // 1부는 10만 자 초과 → 내 펀딩 관리 분량 안내 확인용
                VolumeTone tone = partNumber == 1 ? VolumeTone.OVER : VolumeTone.NORMAL;
                addEpisodes(part, 8, true, tone, random);
                novel.addPart(part);
            }
            return;
        }

        if (!profile.multiPart()) {
            StoryPartStatus partStatus = profile.status() == NovelStatus.COMPLETED
                    ? StoryPartStatus.COMPLETED
                    : StoryPartStatus.SERIALIZING;
            VolumeTone tone = volumeToneFor(novelNumber, 1);
            StoryPart part = StoryPart.create(1, "본편", partStatus);
            addEpisodes(part, episodeCountFor(partStatus, false), profile.serializingAllPublished(), tone, random);
            novel.addPart(part);
            return;
        }

        int partCount = 3;
        for (int partNumber = 1; partNumber <= partCount; partNumber++) {
            StoryPartStatus partStatus = resolveMultiPartStatus(profile, novelNumber, partNumber);
            boolean allPublished = resolveAllPublished(profile, novelNumber, partNumber, partStatus);
            VolumeTone tone = volumeToneFor(novelNumber, partNumber);
            String partTitle = PART_TITLES[(novelNumber + partNumber) % PART_TITLES.length];
            StoryPart part = StoryPart.create(partNumber, partTitle, partStatus);
            addEpisodes(part, episodeCountFor(partStatus, true), allPublished, tone, random);
            novel.addPart(part);
        }
    }

    private StoryPartStatus resolveMultiPartStatus(NovelProfile profile, int novelNumber, int partNumber) {
        if (profile.status() == NovelStatus.COMPLETED) {
            return StoryPartStatus.COMPLETED;
        }
        // 연재 중이어도 일부는 2부·3부까지 공개 완료 상태로 두어 부별 펀딩 시나리오를 만든다.
        int depth = novelNumber % 5;
        if (partNumber == 1) {
            return StoryPartStatus.COMPLETED;
        }
        if (partNumber == 2) {
            if (depth == 0 || depth == 1 || depth == 2) {
                return StoryPartStatus.COMPLETED;
            }
            return StoryPartStatus.SERIALIZING;
        }
        // part 3
        if (depth == 0 || depth == 1) {
            return StoryPartStatus.COMPLETED;
        }
        if (depth == 2) {
            return StoryPartStatus.SERIALIZING;
        }
        return StoryPartStatus.UNPUBLISHED;
    }

    private boolean resolveAllPublished(
            NovelProfile profile,
            int novelNumber,
            int partNumber,
            StoryPartStatus partStatus
    ) {
        if (partStatus == StoryPartStatus.UNPUBLISHED) {
            return false;
        }
        if (partStatus == StoryPartStatus.COMPLETED) {
            return true;
        }
        // SERIALIZING: 일부만 전 회차 공개(펀딩 가능), 나머지는 미공개 회차 포함
        if (profile.status() == NovelStatus.COMPLETED) {
            return true;
        }
        return novelNumber % 3 == 0 || partNumber == 1;
    }

    private int episodeCountFor(StoryPartStatus partStatus, boolean multiPart) {
        if (partStatus == StoryPartStatus.UNPUBLISHED) {
            return multiPart ? 4 : 6;
        }
        if (partStatus == StoryPartStatus.COMPLETED) {
            return multiPart ? 12 : 22;
        }
        return multiPart ? 10 : 18;
    }

    /** 분량 안내 체감용: NORMAL≈가이드, OVER=10만 초과, LIGHT=가볍게 */
    private VolumeTone volumeToneFor(int novelNumber, int partNumber) {
        int key = (novelNumber * 3 + partNumber) % 10;
        if (key == 0) {
            return VolumeTone.OVER;
        }
        if (key <= 3) {
            return VolumeTone.LIGHT;
        }
        return VolumeTone.NORMAL;
    }

    private void addEpisodes(
            StoryPart part,
            int episodeCount,
            boolean allPublished,
            VolumeTone tone,
            Random random
    ) {
        for (int episodeNumber = 1; episodeNumber <= episodeCount; episodeNumber++) {
            EpisodeStatus status;
            if (part.getStatus() == StoryPartStatus.UNPUBLISHED) {
                status = EpisodeStatus.UNPUBLISHED;
            } else if (!allPublished && episodeNumber > episodeCount - 2) {
                status = EpisodeStatus.UNPUBLISHED;
            } else {
                status = EpisodeStatus.PUBLISHED;
            }
            String event = EPISODE_EVENTS[random.nextInt(EPISODE_EVENTS.length)];
            String content = createEpisodeContent(part.getTitle(), event, episodeNumber, tone, episodeCount);
            part.addEpisode(Episode.create(episodeNumber, event, content, status));
        }
    }

    private List<FundingCampaign> createFundings(List<Novel> novels) {
        Random random = new Random(RANDOM_SEED + 7);
        LocalDateTime now = FundingGuide.nowKorea().withSecond(0).withNano(0);
        List<FundingCampaign> campaigns = new ArrayList<>();
        int openIndex = 0;

        for (int novelIndex = 0; novelIndex < novels.size(); novelIndex++) {
            Novel novel = novels.get(novelIndex);
            if (novel.getVisibility() != NovelVisibility.PUBLIC) {
                continue;
            }

            Map<Integer, StoryPart> fundableByNumber = new LinkedHashMap<>();
            for (StoryPart part : novel.getParts()) {
                if (isFundablePart(part)) {
                    fundableByNumber.put(part.getPartNumber(), part);
                }
            }
            if (fundableByNumber.isEmpty()) {
                continue;
            }

            List<Integer> selectedPartNumbers = selectFundingPartNumbers(novel, novelIndex, fundableByNumber.keySet());
            for (Integer partNumber : selectedPartNumbers) {
                StoryPart part = fundableByNumber.get(partNumber);
                if (part == null) {
                    continue;
                }
                campaigns.add(openCampaign(part, random, now, openIndex));
                openIndex++;
            }
        }
        return campaigns;
    }

    /**
     * 부별 펀딩 시나리오:
     * 1부만 / 2부만 / 3부만 / 1+2 / 2+3 / 1+2+3 / 미개시
     */
    private List<Integer> selectFundingPartNumbers(
            Novel novel,
            int novelIndex,
            Set<Integer> fundableNumbers
    ) {
        // 독자 탐색·작가 필터 확인용: 공개 완결 다부작에 진행 펀딩 0건
        if (novel.getId() != null && (novel.getId() == 25L || novel.getId() == 35L)) {
            return List.of();
        }
        if (fundableNumbers.size() >= 5) {
            return pickExisting(fundableNumbers, 1, 2, 3, 4, 5);
        }
        if (!novel.isMultiPart()) {
            return novelIndex % 5 == 1 ? List.of() : List.of(1);
        }

        int pattern = novelIndex % 8;
        return switch (pattern) {
            case 0 -> pickExisting(fundableNumbers, 1);
            case 1 -> pickExisting(fundableNumbers, 2);
            case 2 -> pickExisting(fundableNumbers, 3);
            case 3 -> pickExisting(fundableNumbers, 1, 2);
            case 4 -> pickExisting(fundableNumbers, 2, 3);
            case 5 -> pickExisting(fundableNumbers, 1, 3);
            case 6 -> pickExisting(fundableNumbers, 1, 2, 3);
            default -> List.of(); // 미개시
        };
    }

    private List<Integer> pickExisting(Set<Integer> fundableNumbers, int... wanted) {
        List<Integer> selected = new ArrayList<>();
        for (int partNumber : wanted) {
            if (fundableNumbers.contains(partNumber)) {
                selected.add(partNumber);
            }
        }
        // 원하는 부가 없으면 가능한 첫 부로 대체하지 않고, 비어 있으면 스킵한다.
        return selected;
    }

    private FundingCampaign openCampaign(StoryPart part, Random random, LocalDateTime now, int openIndex) {
        int target = FundingGuide.MIN_TARGET_QUANTITY + 10 + random.nextInt(80);
        int progressBucket = openIndex % 5;
        int current = switch (progressBucket) {
            case 0 -> Math.max(1, target / 10);
            case 1 -> target / 3;
            case 2 -> target / 2;
            case 3 -> (target * 3) / 4;
            default -> Math.min(target - 1, target - random.nextInt(5) - 1);
        };
        // 일부는 수요 0으로 두어 취소 가능 상태를 만든다.
        if (openIndex % 11 == 0) {
            current = 0;
        }
        // 완결부인 일부는 목표 달성 상태로 두어 성공 마감 테스트를 가능하게 한다.
        if (current > 0
                && part.getStatus() == StoryPartStatus.COMPLETED
                && openIndex % 6 == 0) {
            target = FundingGuide.MIN_TARGET_QUANTITY + random.nextInt(15);
            current = target + random.nextInt(5);
        }
        BigDecimal price = BigDecimal.valueOf(12_000L + (random.nextInt(20) * 1_000L));
        LocalDateTime startAt = now
                .minusDays(1L + random.nextInt(18))
                .minusHours(random.nextInt(24))
                .minusMinutes(random.nextInt(60));
        LocalDateTime endAt = startAt
                .plusDays(FundingGuide.MIN_DURATION_DAYS + random.nextInt(12))
                .plusHours(random.nextInt(12))
                .plusMinutes(random.nextInt(60));
        if (endAt.isBefore(now.plusDays(FundingGuide.MIN_DURATION_DAYS))) {
            endAt = now.plusDays(FundingGuide.MIN_DURATION_DAYS + random.nextInt(5))
                    .plusHours(random.nextInt(12))
                    .plusMinutes(random.nextInt(60));
        }
        return FundingCampaign.open(part, target, current, price, startAt, endAt);
    }

    private boolean isFundablePart(StoryPart part) {
        return part.getStatus() != StoryPartStatus.UNPUBLISHED && part.allEpisodesPublished();
    }

    private List<NovelGenre> pickGenres(int novelNumber) {
        List<NovelGenre> longNames = List.of(
                NovelGenre.MODERN_FANTASY,
                NovelGenre.EASTERN_FANTASY,
                NovelGenre.DARK_FANTASY,
                NovelGenre.ROMANCE_FANTASY,
                NovelGenre.HISTORICAL_ROMANCE,
                NovelGenre.ALTERNATE_HISTORY,
                NovelGenre.SPACE_OPERA,
                NovelGenre.BUSINESS,
                NovelGenre.ENTERTAINMENT,
                NovelGenre.HISTORICAL_FICTION,
                NovelGenre.SCIENCE_FICTION,
                NovelGenre.MARTIAL_ARTS
        );
        int bucket = (novelNumber - 1) % 10;
        if (bucket >= 7) {
            int count = bucket == 9 ? 8 : 7;
            int start = (novelNumber - 1) % longNames.size();
            Set<NovelGenre> selected = new LinkedHashSet<>();
            for (int offset = 0; selected.size() < count; offset++) {
                selected.add(longNames.get((start + offset) % longNames.size()));
            }
            return new ArrayList<>(selected);
        }

        NovelGenre[] all = NovelGenre.values();
        int count = switch (bucket) {
            case 0, 1 -> 2;
            case 2, 3 -> 3;
            case 4, 5 -> 4;
            default -> 5;
        };
        Set<NovelGenre> selected = new LinkedHashSet<>();
        int start = (novelNumber * 11) % all.length;
        for (int offset = 0; selected.size() < count; offset++) {
            selected.add(all[(start + offset * 3) % all.length]);
        }
        return new ArrayList<>(selected);
    }

    private String createEpisodeContent(
            String partTitle,
            String event,
            int episodeNumber,
            VolumeTone tone,
            int episodeCount
    ) {
        String unit = partTitle + "의 " + episodeNumber + "번째 이야기에서 " + event + ". "
                + "인물들은 각자의 목적을 숨긴 채 같은 장소에 모였고, 작은 선택 하나가 다음 사건의 방향을 바꾸었다. "
                + "아직 밝혀지지 않은 단서를 확인한 주인공은 물러서지 않기로 했다. "
                + "멀리서 들려오는 소리와 남겨진 기록은 서로 다른 진실을 가리키고 있었다. "
                + "그날의 대화는 쉽게 끝나지 않았다. 누군가는 사실을 감추려 했고, 누군가는 이미 알고 있는 것처럼 침묵했다. "
                + "창밖으로 스치는 바람 소리조차 단서처럼 들렸고, 발걸음은 점점 더 깊은 골목으로 이어졌다. "
                + "결국 남겨진 선택은 하나였다. 위험을 감수하고 앞으로 나아가거나, 안전한 자리에서 진실을 놓치는 것. "
                + "주인공은 주머니 속 낡은 쪽지를 다시 펼쳐 보았다. 흐릿한 글씨 사이로, 아직 끝나지 않은 이야기의 다음 장면이 희미하게 드러나고 있었다. ";

        int targetPartChars = switch (tone) {
            case LIGHT -> 45_000;
            case OVER -> 110_000;
            case NORMAL -> 90_000;
        };
        int perEpisode = Math.max(1_200, targetPartChars / Math.max(1, episodeCount));
        StringBuilder content = new StringBuilder(perEpisode + 64);
        while (content.length() < perEpisode) {
            content.append(unit);
            if (content.length() < perEpisode) {
                content.append('\n').append('\n');
            }
        }
        return content.substring(0, Math.min(content.length(), perEpisode + unit.length() / 4));
    }

    private enum VolumeTone {
        LIGHT,
        NORMAL,
        OVER
    }

    private record NovelProfile(
            NovelStatus status,
            NovelVisibility visibility,
            boolean multiPart,
            boolean serializingAllPublished
    ) {
    }
}
