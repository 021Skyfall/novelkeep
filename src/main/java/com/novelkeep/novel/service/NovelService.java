package com.novelkeep.novel.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.novelkeep.home.domain.ExperienceRole;
import com.novelkeep.member.domain.Member;
import com.novelkeep.member.domain.MemberType;
import com.novelkeep.member.repository.MemberRepository;
import com.novelkeep.novel.domain.Novel;
import com.novelkeep.novel.domain.NovelFavorite;
import com.novelkeep.novel.domain.NovelRecommendation;
import com.novelkeep.novel.domain.NovelStatus;
import com.novelkeep.novel.domain.NovelVisibility;
import com.novelkeep.novel.domain.PartMode;
import com.novelkeep.novel.domain.StoryPart;
import com.novelkeep.novel.domain.StoryPartStatus;
import com.novelkeep.novel.dto.NovelActionResult;
import com.novelkeep.novel.dto.NovelForm;
import com.novelkeep.novel.dto.NovelSearchCriteria;
import com.novelkeep.novel.repository.NovelFavoriteRepository;
import com.novelkeep.novel.repository.NovelRecommendationRepository;
import com.novelkeep.novel.repository.NovelRepository;
import com.novelkeep.novel.repository.NovelSpecifications;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NovelService {

    private static final int PAGE_SIZE = 12;

    private final NovelRepository novelRepository;
    private final NovelRecommendationRepository recommendationRepository;
    private final NovelFavoriteRepository favoriteRepository;
    private final MemberRepository memberRepository;

    public NovelService(
            NovelRepository novelRepository,
            NovelRecommendationRepository recommendationRepository,
            NovelFavoriteRepository favoriteRepository,
            MemberRepository memberRepository
    ) {
        this.novelRepository = novelRepository;
        this.recommendationRepository = recommendationRepository;
        this.favoriteRepository = favoriteRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public Page<Novel> searchPublic(NovelSearchCriteria criteria, ExperienceRole role, Long memberId) {
        Page<Novel> novels = novelRepository.findAll(
                NovelSpecifications.publicBrowse(criteria, role, memberId),
                toPageable(criteria)
        );
        initializeList(novels);
        return novels;
    }

    @Transactional(readOnly = true)
    public Page<Novel> searchOwned(NovelSearchCriteria criteria, Long memberId) {
        Page<Novel> novels = novelRepository.findAll(
                NovelSpecifications.ownedBrowse(criteria, memberId),
                toPageable(criteria)
        );
        initializeList(novels);
        return novels;
    }

    @Transactional(readOnly = true)
    public List<String> findPublicGenres() {
        return novelRepository.findDistinctGenresByVisibility(NovelVisibility.PUBLIC);
    }

    @Transactional(readOnly = true)
    public List<String> findOwnedGenres(Long memberId) {
        return novelRepository.findDistinctGenresByAuthorId(memberId);
    }

    @Transactional(readOnly = true)
    public List<String> findAdminGenres() {
        return novelRepository.findDistinctGenres();
    }

    @Transactional(readOnly = true)
    public Novel findReadableNovel(Long novelId, Long memberId, ExperienceRole role) {
        Novel novel = findNovel(novelId);
        boolean admin = role == ExperienceRole.ADMIN;
        if (!novel.isReadableBy(memberId, admin)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return novel;
    }

    @Transactional(readOnly = true)
    public Novel findOwnedNovel(Long novelId, Long memberId) {
        return novelRepository.findByIdAndAuthorId(novelId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public boolean hasRecommended(Long novelId, Long memberId) {
        return recommendationRepository.existsByMemberIdAndNovelId(memberId, novelId);
    }

    @Transactional(readOnly = true)
    public boolean hasFavorited(Long novelId, Long memberId) {
        return favoriteRepository.existsByMemberIdAndNovelId(memberId, novelId);
    }

    @Transactional(readOnly = true)
    public Set<Long> findRecommendedNovelIds(Long memberId, Iterable<Novel> novels) {
        Set<Long> recommended = new HashSet<>();
        for (Novel novel : novels) {
            if (recommendationRepository.existsByMemberIdAndNovelId(memberId, novel.getId())) {
                recommended.add(novel.getId());
            }
        }
        return recommended;
    }

    @Transactional(readOnly = true)
    public Set<Long> findFavoritedNovelIds(Long memberId, Iterable<Novel> novels) {
        Set<Long> favorited = new HashSet<>();
        for (Novel novel : novels) {
            if (favoriteRepository.existsByMemberIdAndNovelId(memberId, novel.getId())) {
                favorited.add(novel.getId());
            }
        }
        return favorited;
    }

    @Transactional
    public NovelActionResult toggleRecommendation(Long novelId, Long memberId) {
        Novel novel = findNovel(novelId);
        if (novel.getVisibility() != NovelVisibility.PUBLIC) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "공개 작품만 추천할 수 있습니다.");
        }
        if (novel.isOwnedBy(memberId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자신의 작품은 추천할 수 없습니다.");
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        return recommendationRepository.findByMemberIdAndNovelId(memberId, novelId)
                .map(existing -> {
                    recommendationRepository.delete(existing);
                    novelRepository.decreaseRecommendationCount(novelId);
                    return NovelActionResult.of(false, currentRecommendationCount(novelId));
                })
                .orElseGet(() -> {
                    recommendationRepository.save(NovelRecommendation.create(member, novel));
                    novelRepository.increaseRecommendationCount(novelId);
                    return NovelActionResult.of(true, currentRecommendationCount(novelId));
                });
    }

    @Transactional
    public NovelActionResult toggleFavorite(Long novelId, Long memberId) {
        Novel novel = findNovel(novelId);
        if (novel.getVisibility() != NovelVisibility.PUBLIC) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "공개 작품만 즐겨찾기할 수 있습니다.");
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        return favoriteRepository.findByMemberIdAndNovelId(memberId, novelId)
                .map(existing -> {
                    favoriteRepository.delete(existing);
                    return NovelActionResult.favorite(false);
                })
                .orElseGet(() -> {
                    favoriteRepository.save(NovelFavorite.create(member, novel));
                    return NovelActionResult.favorite(true);
                });
    }

    @Transactional
    public Long create(Long memberId, NovelForm form) {
        Member author = findAuthor(memberId);
        Novel novel = createNovel(author, form);
        return novelRepository.save(novel).getId();
    }

    @Transactional
    public void update(Long novelId, Long memberId, NovelForm form) {
        Novel novel = findOwnedNovel(novelId, memberId);
        novel.update(
                normalize(form.getTitle()),
                normalize(form.getPenName()),
                normalize(form.getGenre()),
                normalize(form.getSynopsis()),
                form.getStatus(),
                form.getVisibility()
        );
    }

    @Transactional
    public void delete(Long novelId, Long memberId) {
        Novel novel = findOwnedNovel(novelId, memberId);
        recommendationRepository.deleteByNovelId(novelId);
        favoriteRepository.deleteByNovelId(novelId);
        novelRepository.delete(novel);
    }

    @Transactional
    public void createSamplesIfEmpty() {
        if (novelRepository.count() > 0) {
            return;
        }

        Member author = memberRepository.findByMemberType(MemberType.AUTHOR)
                .orElseGet(() -> memberRepository.save(Member.create(MemberType.AUTHOR)));

        saveSample(author, "회귀한 서기관의 기록", "푸른잉크", "판타지",
                "제국의 멸망을 기록한 서기관이 열두 해 전으로 돌아왔다.",
                NovelStatus.SERIALIZING, PartMode.SINGLE, "본편", StoryPartStatus.SERIALIZING);
        saveSample(author, "달빛 아래 마지막 우체국", "새벽편지", "현대 판타지",
                "사라진 사람에게 보내는 편지를 배달하는 야간 우체국 이야기.",
                NovelStatus.SERIALIZING, PartMode.MULTI, "마지막 배달", StoryPartStatus.COMPLETED);
        saveSample(author, "북부 공작의 계약 서재", "은하수책방", "로맨스 판타지",
                "책을 펼칠 때마다 계약 상대의 기억이 한 장씩 나타난다.",
                NovelStatus.SERIALIZING, PartMode.MULTI, "계약의 시작", StoryPartStatus.SERIALIZING);
        saveSample(author, "별을 접는 방법", "작은궤도", "로맨스",
                "매일 밤 별을 접어 보내던 두 사람이 마지막 편지에서 만난다.",
                NovelStatus.COMPLETED, PartMode.SINGLE, "본편", StoryPartStatus.COMPLETED);
    }

    private Pageable toPageable(NovelSearchCriteria criteria) {
        Sort sort;
        if (criteria.isRecommendSort()) {
            sort = Sort.by(Sort.Order.desc("recommendationCount"), Sort.Order.desc("updatedAt"));
        } else if (criteria.isTitleSort()) {
            sort = Sort.by(Sort.Order.asc("title"), Sort.Order.desc("updatedAt"));
        } else {
            sort = Sort.by(Sort.Order.desc("updatedAt"));
        }
        return PageRequest.of(criteria.getPage(), PAGE_SIZE, sort);
    }

    private void initializeList(Page<Novel> novels) {
        novels.forEach(novel -> {
            novel.getAuthor().getId();
            novel.getParts().size();
        });
    }

    private Novel createNovel(Member author, NovelForm form) {
        Novel novel = Novel.create(
                author,
                normalize(form.getTitle()),
                normalize(form.getPenName()),
                normalize(form.getGenre()),
                normalize(form.getSynopsis()),
                form.getStatus(),
                form.getVisibility(),
                form.getPartMode()
        );

        String partTitle = form.getPartMode() == PartMode.SINGLE
                ? "본편"
                : normalize(form.getFirstPartTitle());
        if (form.getPartMode() == PartMode.MULTI && partTitle.isBlank()) {
            throw new IllegalArgumentException("부 구분을 사용하면 첫 부 제목이 필요합니다.");
        }

        novel.addPart(StoryPart.create(1, partTitle, toPartStatus(form.getStatus())));
        return novel;
    }

    private void saveSample(
            Member author,
            String title,
            String penName,
            String genre,
            String synopsis,
            NovelStatus novelStatus,
            PartMode partMode,
            String partTitle,
            StoryPartStatus partStatus
    ) {
        Novel novel = Novel.create(
                author, title, penName, genre, synopsis,
                novelStatus, NovelVisibility.PUBLIC, partMode
        );
        novel.addPart(StoryPart.create(1, partTitle, partStatus));
        novelRepository.save(novel);
    }

    private Novel findNovel(Long novelId) {
        return novelRepository.findById(novelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private long currentRecommendationCount(Long novelId) {
        return findNovel(novelId).getRecommendationCount();
    }

    private Member findAuthor(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (member.getMemberType() != MemberType.AUTHOR && member.getMemberType() != MemberType.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return member;
    }

    private StoryPartStatus toPartStatus(NovelStatus novelStatus) {
        return switch (novelStatus) {
            case SERIALIZING -> StoryPartStatus.SERIALIZING;
            case COMPLETED -> StoryPartStatus.COMPLETED;
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
