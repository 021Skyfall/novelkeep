package com.novelkeep.funding.repository;

import java.util.Collection;
import java.util.List;

import com.novelkeep.funding.domain.FundingParticipation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FundingParticipationRepository extends JpaRepository<FundingParticipation, Long> {

    boolean existsByCampaignId(Long campaignId);

    boolean existsByCampaignIdAndMemberId(Long campaignId, Long memberId);

    @Query("""
            select p.campaign.id from FundingParticipation p
            where p.member.id = :memberId
              and p.campaign.id in :campaignIds
            """)
    List<Long> findCampaignIdsByMemberIdAndCampaignIdIn(
            @Param("memberId") Long memberId,
            @Param("campaignIds") Collection<Long> campaignIds
    );
}
