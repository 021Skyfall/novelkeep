package com.novelkeep.novel.domain;

import java.util.EnumSet;
import java.util.Set;

public enum NovelGenre {

    GENERAL_FICTION("일반소설"),
    LITERARY_FICTION("문학"),
    FANTASY("판타지"),
    MODERN_FANTASY("현대 판타지"),
    EASTERN_FANTASY("동양 판타지"),
    DARK_FANTASY("다크 판타지"),
    ROMANCE("로맨스"),
    ROMANCE_FANTASY("로맨스 판타지"),
    HISTORICAL_ROMANCE("시대물 로맨스"),
    MARTIAL_ARTS("무협"),
    FUSION("퓨전"),
    GAME("게임"),
    SPORTS("스포츠"),
    ALTERNATE_HISTORY("대체역사"),
    HISTORICAL_FICTION("역사소설"),
    SCIENCE_FICTION("SF"),
    SPACE_OPERA("스페이스 오페라"),
    DYSTOPIA("디스토피아"),
    MYSTERY("미스터리"),
    DETECTIVE("추리"),
    THRILLER("스릴러"),
    CRIME("범죄"),
    HORROR("공포"),
    OCCULT("오컬트"),
    APOCALYPSE("아포칼립스"),
    DISASTER("재난"),
    MILITARY("밀리터리"),
    WAR("전쟁"),
    DRAMA("드라마"),
    DAILY_LIFE("일상"),
    HEALING("힐링"),
    YOUTH("청춘"),
    SCHOOL("학원"),
    GROWTH("성장"),
    ADVENTURE("모험"),
    ACTION("액션"),
    COMEDY("코미디"),
    SATIRE("풍자"),
    MEDICAL("의학"),
    LEGAL("법정"),
    PROFESSIONAL("직업"),
    BUSINESS("경제·경영"),
    COOKING("요리"),
    ENTERTAINMENT("연예계"),
    MUSIC("음악"),
    FAMILY("가족"),
    BOYS_LOVE("BL"),
    GIRLS_LOVE("GL"),
    CHILDREN("아동"),
    FAIRY_TALE("동화");

    public static final int MAX_PER_NOVEL = 8;

    private static final Set<NovelGenre> PRIMARY = EnumSet.of(
            FANTASY,
            MODERN_FANTASY,
            ROMANCE,
            ROMANCE_FANTASY,
            MARTIAL_ARTS,
            SCIENCE_FICTION,
            MYSTERY,
            DRAMA,
            HORROR,
            HEALING,
            BOYS_LOVE,
            ACTION
    );

    private final String displayName;

    NovelGenre(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isPrimary() {
        return PRIMARY.contains(this);
    }
}
