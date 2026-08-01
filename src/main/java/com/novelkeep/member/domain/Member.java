package com.novelkeep.member.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_type", nullable = false, length = 20)
    private MemberType memberType;

    @Column(name = "display_name", length = 40)
    private String displayName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Member() {
    }

    public static Member create(MemberType memberType) {
        Member member = new Member();
        member.memberType = memberType;
        return member;
    }

    public static Member createMockReader(String displayName) {
        Member member = new Member();
        member.memberType = MemberType.READER;
        member.displayName = displayName;
        return member;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public MemberType getMemberType() {
        return memberType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String resolveLabel() {
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        return memberType.getDisplayName();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
