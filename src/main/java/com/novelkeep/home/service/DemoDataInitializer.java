package com.novelkeep.home.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.novelkeep.member.domain.Member;
import com.novelkeep.member.domain.MemberType;
import com.novelkeep.member.repository.MemberRepository;
import com.novelkeep.novel.domain.Episode;
import com.novelkeep.novel.domain.EpisodeStatus;
import com.novelkeep.novel.domain.Novel;
import com.novelkeep.novel.domain.NovelGenre;
import com.novelkeep.novel.domain.NovelStatus;
import com.novelkeep.novel.domain.NovelVisibility;
import com.novelkeep.novel.domain.PartMode;
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

    public DemoDataInitializer(
            MemberRepository memberRepository,
            NovelRepository novelRepository
    ) {
        this.memberRepository = memberRepository;
        this.novelRepository = novelRepository;
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
            PartMode partMode = status == NovelStatus.COMPLETED || number % 4 != 1
                    ? PartMode.MULTI
                    : PartMode.SINGLE;

            NovelGenre genre = NovelGenre.values()[(number - 1) % NovelGenre.values().length];
            String title = TITLE_PREFIXES[metadataRandom.nextInt(TITLE_PREFIXES.length)]
                    + " " + TITLE_SUBJECTS[metadataRandom.nextInt(TITLE_SUBJECTS.length)]
                    + " " + String.format("%02d", number);
            String penName = PEN_NAMES[metadataRandom.nextInt(PEN_NAMES.length)];
            String synopsis = genre.getDisplayName() + " 세계에서 "
                    + STORY_KEYWORDS[number % STORY_KEYWORDS.length]
                    + "을 추적하는 인물들의 선택과 성장을 그린 이야기.";

            Novel novel = Novel.create(
                    owner, title, penName, genre, synopsis,
                    status, visibility, partMode
            );
            if (partMode == PartMode.SINGLE) {
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
                partStatus = StoryPartStatus.DRAFT;
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
            String title = episodeNumber + "화. " + event;
            String content = createEpisodeContent(part.getTitle(), event, episodeNumber);
            part.addEpisode(Episode.create(episodeNumber, title, content, status));
        }
    }

    private EpisodeStatus resolveEpisodeStatus(
            StoryPartStatus partStatus,
            int episodeNumber,
            int episodeCount
    ) {
        if (partStatus == StoryPartStatus.DRAFT) {
            return EpisodeStatus.DRAFT;
        }
        if (partStatus == StoryPartStatus.SERIALIZING && episodeNumber > episodeCount - 2) {
            return EpisodeStatus.DRAFT;
        }
        return EpisodeStatus.PUBLISHED;
    }

    private String createEpisodeContent(String partTitle, String event, int episodeNumber) {
        return partTitle + "의 " + episodeNumber + "번째 이야기에서 " + event + ". "
                + "인물들은 각자의 목적을 숨긴 채 같은 장소에 모였고, 작은 선택 하나가 다음 사건의 방향을 바꾸었다.\n\n"
                + "아직 밝혀지지 않은 단서를 확인한 주인공은 물러서지 않기로 했다. "
                + "멀리서 들려오는 소리와 남겨진 기록은 서로 다른 진실을 가리키고 있었다.";
    }

}
