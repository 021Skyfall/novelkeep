package com.novelkeep.novel.service;

import java.util.List;

import com.novelkeep.home.domain.ExperienceRole;
import com.novelkeep.member.domain.Member;
import com.novelkeep.member.repository.MemberRepository;
import com.novelkeep.novel.domain.Episode;
import com.novelkeep.novel.domain.EpisodeBookmark;
import com.novelkeep.novel.domain.Novel;
import com.novelkeep.novel.dto.EpisodeBookmarkResult;
import com.novelkeep.novel.repository.EpisodeBookmarkRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EpisodeBookmarkService {

    private final EpisodeBookmarkRepository bookmarkRepository;
    private final StoryContentService storyContentService;
    private final MemberRepository memberRepository;

    public EpisodeBookmarkService(
            EpisodeBookmarkRepository bookmarkRepository,
            StoryContentService storyContentService,
            MemberRepository memberRepository
    ) {
        this.bookmarkRepository = bookmarkRepository;
        this.storyContentService = storyContentService;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public EpisodeBookmarkResult toggle(Long episodeId, Long memberId, ExperienceRole role) {
        Episode episode = storyContentService.findReadableEpisode(episodeId, memberId, role);
        Novel novel = episode.getStoryPart().getNovel();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        return bookmarkRepository.findByMemberIdAndNovelId(memberId, novel.getId())
                .map(existing -> {
                    if (existing.getEpisode().getId().equals(episodeId)) {
                        bookmarkRepository.delete(existing);
                        return EpisodeBookmarkResult.removed();
                    }
                    existing.moveTo(episode);
                    return savedResult(episode, novel);
                })
                .orElseGet(() -> {
                    bookmarkRepository.save(EpisodeBookmark.create(member, novel, episode));
                    return savedResult(episode, novel);
                });
    }

    private EpisodeBookmarkResult savedResult(Episode episode, Novel novel) {
        novel.getParts().size();
        String partLabel = null;
        if (novel.isMultiPart()) {
            partLabel = episode.getStoryPart().getPartNumber() + "부";
        }
        return EpisodeBookmarkResult.saved(
                episode.getId(),
                episode.getEpisodeNumber(),
                novel.getTitle(),
                partLabel
        );
    }

    @Transactional(readOnly = true)
    public EpisodeBookmark findReadableBookmark(Long novelId, Long memberId, ExperienceRole role) {
        return bookmarkRepository.findByMemberIdAndNovelId(memberId, novelId)
                .filter(bookmark -> isContinueReadable(bookmark.getEpisode(), memberId, role))
                .map(bookmark -> {
                    bookmark.getEpisode().getEpisodeNumber();
                    return bookmark;
                })
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean isBookmarkedEpisode(Long novelId, Long episodeId, Long memberId) {
        return bookmarkRepository.findByMemberIdAndNovelId(memberId, novelId)
                .map(bookmark -> bookmark.getEpisode().getId().equals(episodeId))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<EpisodeBookmark> findReadableBookmarks(Long memberId, ExperienceRole role) {
        return bookmarkRepository.findAllByMemberIdOrderByUpdatedAtDesc(memberId).stream()
                .filter(bookmark -> isContinueReadable(bookmark.getEpisode(), memberId, role))
                .peek(bookmark -> {
                    bookmark.getNovel().getTitle();
                    bookmark.getEpisode().getTitle();
                })
                .toList();
    }

    @Transactional
    public void delete(Long memberId, Long novelId) {
        bookmarkRepository.deleteByMemberIdAndNovelId(memberId, novelId);
    }

    private boolean isContinueReadable(Episode episode, Long memberId, ExperienceRole role) {
        try {
            storyContentService.findReadableEpisode(episode.getId(), memberId, role);
            return true;
        } catch (ResponseStatusException ex) {
            return false;
        }
    }
}
