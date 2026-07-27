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
    @Column(name = "member_type", nullable = false, unique = true, length = 20)
    private MemberType memberType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Member() {
    }

    public static Member create(MemberType memberType) {
        Member member = new Member();
        member.memberType = memberType;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
