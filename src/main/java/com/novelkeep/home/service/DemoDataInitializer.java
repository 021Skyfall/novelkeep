package com.novelkeep.home.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.novelkeep.funding.domain.FundingCampaign;
import com.novelkeep.funding.repository.FundingCampaignRepository;
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

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataInitializer implements ApplicationRunner {

    private static final int NOVEL_COUNT = 50;
    private static final int OPEN_FUNDING_COUNT = 5;
    private static final long RANDOM_SEED = 20260731L;

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

    public DemoDataInitializer(
            MemberRepository memberRepository,
            NovelRepository novelRepository,
            FundingCampaignRepository fundingCampaignRepository
    ) {
        this.memberRepository = memberRepository;
        this.novelRepository = novelRepository;
        this.fundingCampaignRepository = fundingCampaignRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (memberRepository.count() > 0 || novelRepository.count() > 0) {
            return;
        }

        memberRepository.save(Member.create(MemberType.READER));
        Member author = memberRepository.save(Member.create(MemberType.AUTHOR));
        Member admin = memberRepository.save(Member.create(MemberType.ADMIN));

        List<Novel> novels = createNovels(author, admin);
        novelRepository.saveAll(novels);
        fundingCampaignRepository.saveAll(createOpenFundings(novels));
    }

    private List<Novel> createNovels(Member author, Member admin) {
        Random metadataRandom = new Random(RANDOM_SEED);
        Random structureRandom = new Random(RANDOM_SEED + 1);
        List<Novel> novels = new ArrayList<>();

        for (int number = 1; number <= NOVEL_COUNT; number++) {
            Member owner = number % 6 == 0 ? admin : author;
            NovelStatus status = number % 3 == 0
                    ? NovelStatus.COMPLETED
                    : NovelStatus.SERIALIZING;
            NovelVisibility visibility = number % 7 == 0
                    ? NovelVisibility.PRIVATE
                    : NovelVisibility.PUBLIC;
            boolean multipleParts = status == NovelStatus.COMPLETED || number % 4 != 1;

            List<NovelGenre> genres = pickGenres(number);
            String title = TITLE_PREFIXES[metadataRandom.nextInt(TITLE_PREFIXES.length)]
                    + " " + TITLE_SUBJECTS[metadataRandom.nextInt(TITLE_SUBJECTS.length)]
                    + " " + String.format("%02d", number);
            String penName = PEN_NAMES[metadataRandom.nextInt(PEN_NAMES.length)];
            String synopsis = genres.getFirst().getDisplayName() + " 세계에서 "
                    + STORY_KEYWORDS[number % STORY_KEYWORDS.length]
                    + "을 추적하는 인물들의 선택과 성장을 그린 이야기.";

            Novel novel = Novel.create(
                    owner, title, penName, genres, synopsis,
                    status, visibility
            );
            if (!multipleParts) {
                addSinglePart(novel, status, structureRandom);
            } else {
                addMultipleParts(novel, number, status, structureRandom);
            }
            novels.add(novel);
        }
        return novels;
    }

    private void addSinglePart(Novel novel, NovelStatus status, Random random) {
        StoryPartStatus partStatus = status == NovelStatus.COMPLETED
                ? StoryPartStatus.COMPLETED
                : StoryPartStatus.SERIALIZING;
        StoryPart part = StoryPart.create(1, "본편", partStatus);
        addEpisodes(part, 20 + random.nextInt(5), random);
        novel.addPart(part);
    }

    private void addMultipleParts(
            Novel novel,
            int novelNumber,
            NovelStatus novelStatus,
            Random random
    ) {
        int partCount = 3;
        int completedPartCount;
        if (novelStatus == NovelStatus.COMPLETED) {
            completedPartCount = partCount;
        } else if (novelNumber % 2 == 0) {
            completedPartCount = 1 + random.nextInt(partCount - 1);
        } else {
            completedPartCount = 0;
        }

        for (int partNumber = 1; partNumber <= partCount; partNumber++) {
            StoryPartStatus partStatus;
            if (partNumber <= completedPartCount) {
                partStatus = StoryPartStatus.COMPLETED;
            } else if (partNumber == completedPartCount + 1) {
                partStatus = StoryPartStatus.SERIALIZING;
            } else {
                partStatus = StoryPartStatus.UNPUBLISHED;
            }

            String partTitle = PART_TITLES[(novelNumber + partNumber) % PART_TITLES.length];
            StoryPart part = StoryPart.create(partNumber, partTitle, partStatus);
            addEpisodes(part, 7 + random.nextInt(3), random);
            novel.addPart(part);
        }
    }

    private void addEpisodes(StoryPart part, int episodeCount, Random random) {
        for (int episodeNumber = 1; episodeNumber <= episodeCount; episodeNumber++) {
            EpisodeStatus status = resolveEpisodeStatus(part.getStatus(), episodeNumber, episodeCount);
            String event = EPISODE_EVENTS[random.nextInt(EPISODE_EVENTS.length)];
            String title = event;
            String content = createEpisodeContent(part.getTitle(), event, episodeNumber);
            part.addEpisode(Episode.create(episodeNumber, title, content, status));
        }
    }

    private List<FundingCampaign> createOpenFundings(List<Novel> novels) {
        List<StoryPart> candidates = new ArrayList<>();
        for (Novel novel : novels) {
            if (novel.getVisibility() != NovelVisibility.PUBLIC) {
                continue;
            }
            for (StoryPart part : novel.getParts()) {
                if (part.getStatus() == StoryPartStatus.COMPLETED) {
                    candidates.add(part);
                }
            }
        }

        LocalDateTime now = LocalDateTime.now();
        List<FundingCampaign> campaigns = new ArrayList<>();
        int count = Math.min(OPEN_FUNDING_COUNT, candidates.size());
        for (int i = 0; i < count; i++) {
            StoryPart part = candidates.get(i);
            int target = 40 + (i * 10);
            int current = 8 + (i * 7);
            BigDecimal price = BigDecimal.valueOf(15000L + (i * 1000L));
            campaigns.add(FundingCampaign.open(
                    part,
                    target,
                    Math.min(current, target - 1),
                    price,
                    now.minusDays(3 + i),
                    now.plusDays(14 + i)
            ));
        }
        return campaigns;
    }

    private List<NovelGenre> pickGenres(int novelNumber) {
        // 카드 2줄 초과 `...` 확인용: 일부는 긴 이름 장르 7~8개
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

    private EpisodeStatus resolveEpisodeStatus(
            StoryPartStatus partStatus,
            int episodeNumber,
            int episodeCount
    ) {
        if (partStatus == StoryPartStatus.UNPUBLISHED) {
            return EpisodeStatus.UNPUBLISHED;
        }
        if (partStatus == StoryPartStatus.SERIALIZING && episodeNumber > episodeCount - 2) {
            return EpisodeStatus.UNPUBLISHED;
        }
        return EpisodeStatus.PUBLISHED;
    }

    private String createEpisodeContent(String partTitle, String event, int episodeNumber) {
        String paragraph = partTitle + "의 " + episodeNumber + "번째 이야기에서 " + event + ". "
                + "인물들은 각자의 목적을 숨긴 채 같은 장소에 모였고, 작은 선택 하나가 다음 사건의 방향을 바꾸었다.\n\n"
                + "아직 밝혀지지 않은 단서를 확인한 주인공은 물러서지 않기로 했다. "
                + "멀리서 들려오는 소리와 남겨진 기록은 서로 다른 진실을 가리키고 있었다.\n\n"
                + "그날의 대화는 쉽게 끝나지 않았다. 누군가는 사실을 감추려 했고, 누군가는 이미 알고 있는 것처럼 침묵했다. "
                + "창밖으로 스치는 바람 소리조차 단서처럼 들렸고, 발걸음은 점점 더 깊은 골목으로 이어졌다.\n\n"
                + "결국 남겨진 선택은 하나였다. 위험을 감수하고 앞으로 나아가거나, 안전한 자리에서 진실을 놓치는 것. "
                + "주인공은 주머니 속 낡은 쪽지를 다시 펼쳐 보았다. 흐릿한 글씨 사이로, 아직 끝나지 않은 이야기의 다음 장면이 희미하게 드러나고 있었다.";
        return paragraph + "\n\n" + paragraph + "\n\n" + paragraph + "\n\n" + paragraph;
    }

}
