package com.novelkeep.member.service;

import com.novelkeep.member.domain.Member;
import com.novelkeep.member.domain.MemberType;
import com.novelkeep.member.repository.MemberRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public Member getOrCreateExperienceMember(MemberType memberType) {
        return memberRepository.findByMemberType(memberType)
                .orElseGet(() -> memberRepository.save(Member.create(memberType)));
    }
}
