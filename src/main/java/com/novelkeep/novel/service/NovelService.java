package com.novelkeep.novel.service;

import java.util.List;

import com.novelkeep.member.domain.Member;
import com.novelkeep.member.domain.MemberType;
import com.novelkeep.member.repository.MemberRepository;
import com.novelkeep.novel.domain.Novel;
import com.novelkeep.novel.domain.NovelStatus;
import com.novelkeep.novel.domain.PartMode;
import com.novelkeep.novel.domain.StoryPart;
import com.novelkeep.novel.domain.StoryPartStatus;
import com.novelkeep.novel.dto.NovelForm;
import com.novelkeep.novel.repository.NovelRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NovelService {

    private final NovelRepository novelRepository;
    private final MemberRepository memberRepository;

    public NovelService(NovelRepository novelRepository, MemberRepository memberRepository) {
        this.novelRepository = novelRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public List<Novel> findPublicNovels() {
        return novelRepository.findAllByStatusNotOrderByUpdatedAtDesc(NovelStatus.DRAFT);
    }

    @Transactional(readOnly = true)
    public List<Novel> findOwnedNovels(Long memberId) {
        return novelRepository.findAllByAuthorIdOrderByUpdatedAtDesc(memberId);
    }

    @Transactional(readOnly = true)
    public Novel findReadableNovel(Long novelId, Long memberId) {
        Novel novel = findNovel(novelId);
        boolean isOwner = novel.getAuthor().getId().equals(memberId);
        if (novel.getStatus() == NovelStatus.DRAFT && !isOwner) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return novel;
    }

    @Transactional(readOnly = true)
    public Novel findOwnedNovel(Long novelId, Long memberId) {
        return novelRepository.findByIdAndAuthorId(novelId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
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
                form.getStatus()
        );
    }

    @Transactional
    public void delete(Long novelId, Long memberId) {
        Novel novel = findOwnedNovel(novelId, memberId);
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

    private Novel createNovel(Member author, NovelForm form) {
        Novel novel = Novel.create(
                author,
                normalize(form.getTitle()),
                normalize(form.getPenName()),
                normalize(form.getGenre()),
                normalize(form.getSynopsis()),
                form.getStatus(),
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
        Novel novel = Novel.create(author, title, penName, genre, synopsis, novelStatus, partMode);
        novel.addPart(StoryPart.create(1, partTitle, partStatus));
        novelRepository.save(novel);
    }

    private Novel findNovel(Long novelId) {
        return novelRepository.findById(novelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
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
            case DRAFT -> StoryPartStatus.DRAFT;
            case SERIALIZING -> StoryPartStatus.SERIALIZING;
            case COMPLETED -> StoryPartStatus.COMPLETED;
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
