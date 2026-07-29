package com.novelkeep.member.service;

import com.novelkeep.member.domain.Member;
import com.novelkeep.member.domain.MemberType;
import com.novelkeep.member.repository.MemberRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MemberService {

    private static final int SEED_WAIT_ATTEMPTS = 50;
    private static final long SEED_WAIT_MILLIS = 100L;

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member findExperienceMember(MemberType memberType) {
        for (int attempt = 0; attempt < SEED_WAIT_ATTEMPTS; attempt++) {
            Member member = memberRepository.findByMemberType(memberType).orElse(null);
            if (member != null) {
                return member;
            }
            waitForSeedData();
        }
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "체험 데이터를 준비하고 있습니다. 잠시 후 다시 시도해 주세요."
        );
    }

    private void waitForSeedData() {
        try {
            Thread.sleep(SEED_WAIT_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "체험 데이터 준비가 중단되었습니다.",
                    exception
            );
        }
    }
}
