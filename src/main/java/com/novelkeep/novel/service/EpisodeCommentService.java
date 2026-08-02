package com.novelkeep.novel.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.novelkeep.home.domain.ExperienceRole;
import com.novelkeep.member.domain.Member;
import com.novelkeep.member.repository.MemberRepository;
import com.novelkeep.novel.domain.Episode;
import com.novelkeep.novel.domain.EpisodeComment;
import com.novelkeep.novel.dto.EpisodeCommentForm;
import com.novelkeep.novel.repository.EpisodeCommentRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EpisodeCommentService {

    private final EpisodeCommentRepository commentRepository;
    private final StoryContentService storyContentService;
    private final MemberRepository memberRepository;

    public EpisodeCommentService(
            EpisodeCommentRepository commentRepository,
            StoryContentService storyContentService,
            MemberRepository memberRepository
    ) {
        this.commentRepository = commentRepository;
        this.storyContentService = storyContentService;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public List<EpisodeComment> listReadable(Long episodeId, Long memberId, ExperienceRole role) {
        storyContentService.findReadableEpisode(episodeId, memberId, role);
        return commentRepository.findRootsWithRepliesByEpisodeId(episodeId).stream()
                .filter(EpisodeComment::isVisibleInThread)
                .toList();
    }

    @Transactional(readOnly = true)
    public Episode findReadableEpisodeRef(Long episodeId, Long memberId, ExperienceRole role) {
        return storyContentService.findReadableEpisode(episodeId, memberId, role);
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> countByEpisodeIds(Collection<Long> episodeIds) {
        Map<Long, Long> counts = new HashMap<>();
        if (episodeIds == null || episodeIds.isEmpty()) {
            return counts;
        }
        for (Object[] row : commentRepository.countGroupedByEpisodeIds(episodeIds)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Transactional
    public EpisodeComment create(
            Long episodeId,
            Long memberId,
            ExperienceRole role,
            EpisodeCommentForm form
    ) {
        Episode episode = storyContentService.findReadableEpisode(episodeId, memberId, role);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (form.getParentId() == null) {
            return commentRepository.save(EpisodeComment.create(episode, member, form.getContent()));
        }

        EpisodeComment parent = commentRepository.findDetailById(form.getParentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!parent.getEpisode().getId().equals(episodeId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "다른 회차의 댓글에는 답글할 수 없습니다.");
        }
        if (!parent.isRoot()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "대댓글에는 다시 답글을 달 수 없습니다.");
        }
        return commentRepository.save(EpisodeComment.reply(parent, member, form.getContent()));
    }

    @Transactional
    public EpisodeComment update(
            Long commentId,
            Long memberId,
            ExperienceRole role,
            EpisodeCommentForm form
    ) {
        EpisodeComment comment = findOwnedActive(commentId, memberId);
        storyContentService.findReadableEpisode(comment.getEpisode().getId(), memberId, role);
        comment.updateContent(form.getContent());
        return comment;
    }

    @Transactional(readOnly = true)
    public List<EpisodeComment> listMyComments(Long memberId) {
        if (memberId == null) {
            return List.of();
        }
        return commentRepository.findActiveByMemberId(memberId);
    }

    @Transactional
    public Long delete(Long commentId, Long memberId, ExperienceRole role) {
        EpisodeComment comment = findOwnedActive(commentId, memberId);
        Long episodeId = comment.getEpisode().getId();
        storyContentService.findReadableEpisode(episodeId, memberId, role);

        if (comment.isRoot() && comment.hasActiveReplies()) {
            comment.softDelete();
            return episodeId;
        }

        if (!comment.isRoot() && comment.getParent() != null) {
            comment.getParent().getReplies().remove(comment);
        }
        commentRepository.delete(comment);
        return episodeId;
    }

    @Transactional(readOnly = true)
    public Long findOwnedEpisodeId(Long commentId, Long memberId) {
        return findOwnedActive(commentId, memberId).getEpisode().getId();
    }

    private EpisodeComment findOwnedActive(Long commentId, Long memberId) {
        EpisodeComment comment = commentRepository.findDetailById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!comment.getMember().getId().equals(memberId) || comment.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return comment;
    }
}

