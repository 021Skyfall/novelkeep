package com.novelkeep.member.repository;

import java.util.Optional;

import com.novelkeep.member.domain.Member;
import com.novelkeep.member.domain.MemberType;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByMemberType(MemberType memberType);
}
