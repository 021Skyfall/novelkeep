package com.novelkeep.novel.domain;

import java.time.LocalDateTime;

import com.novelkeep.member.domain.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "novel_favorite",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_novel_favorite_member_novel",
                columnNames = {"member_id", "novel_id"}
        )
)
public class NovelFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "novel_id", nullable = false)
    private Novel novel;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected NovelFavorite() {
    }

    public static NovelFavorite create(Member member, Novel novel) {
        NovelFavorite favorite = new NovelFavorite();
        favorite.member = member;
        favorite.novel = novel;
        return favorite;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public Novel getNovel() {
        return novel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
