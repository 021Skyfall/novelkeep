package com.novelkeep.novel.domain;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.novelkeep.member.domain.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "episode_comment")
public class EpisodeComment {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "episode_id", nullable = false)
    private Episode episode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private EpisodeComment parent;

    @OneToMany(mappedBy = "parent")
    @OrderBy("createdAt ASC")
    private List<EpisodeComment> replies = new ArrayList<>();

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected EpisodeComment() {
    }

    public static EpisodeComment create(Episode episode, Member member, String content) {
        EpisodeComment comment = new EpisodeComment();
        comment.episode = episode;
        comment.member = member;
        comment.content = content.trim();
        return comment;
    }

    public static EpisodeComment reply(EpisodeComment parent, Member member, String content) {
        if (parent.getParent() != null) {
            throw new IllegalArgumentException("대댓글에는 다시 답글을 달 수 없습니다.");
        }
        EpisodeComment comment = create(parent.getEpisode(), member, content);
        comment.parent = parent;
        parent.replies.add(comment);
        return comment;
    }

    public void updateContent(String content) {
        if (isDeleted()) {
            throw new IllegalStateException("삭제된 댓글은 수정할 수 없습니다.");
        }
        this.content = content.trim();
    }

    public void softDelete() {
        if (isDeleted()) {
            return;
        }
        this.deletedAt = LocalDateTime.now(KOREA_ZONE);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public boolean hasActiveReplies() {
        return replies.stream().anyMatch(reply -> !reply.isDeleted());
    }

    public boolean isVisibleInThread() {
        if (!isDeleted()) {
            return true;
        }
        return isRoot() && hasActiveReplies();
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now(KOREA_ZONE);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now(KOREA_ZONE);
    }

    public Long getId() {
        return id;
    }

    public Episode getEpisode() {
        return episode;
    }

    public Member getMember() {
        return member;
    }

    public EpisodeComment getParent() {
        return parent;
    }

    public List<EpisodeComment> getReplies() {
        return replies;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
