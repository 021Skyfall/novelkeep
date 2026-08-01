package com.novelkeep.funding.repository;

import java.util.Collection;
import java.util.List;

import com.novelkeep.funding.domain.FundingParticipation;
import com.novelkeep.funding.domain.FundingPaymentStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FundingParticipationRepository extends JpaRepository<FundingParticipation, Long> {

    boolean existsByCampaignId(Long campaignId);

    boolean existsByCampaignIdAndMemberId(Long campaignId, Long memberId);

    List<FundingParticipation> findByCampaignId(Long campaignId);

    List<FundingParticipation> findByCampaignIdAndPaymentStatus(
            Long campaignId,
            FundingPaymentStatus paymentStatus
    );

    @Query("""
            select p from FundingParticipation p
             where p.campaign.id = :campaignId
               and p.member.id = :memberId
            """)
    java.util.Optional<FundingParticipation> findByCampaignIdAndMemberId(
            @Param("campaignId") Long campaignId,
            @Param("memberId") Long memberId
    );

    @Query("""
            select p.campaign.id from FundingParticipation p
            where p.member.id = :memberId
              and p.campaign.id in :campaignIds
            """)
    List<Long> findCampaignIdsByMemberIdAndCampaignIdIn(
            @Param("memberId") Long memberId,
            @Param("campaignIds") Collection<Long> campaignIds
    );

    @Query("""
            select distinct p
              from FundingParticipation p
              join fetch p.campaign c
              join fetch c.storyPart sp
              join fetch sp.novel n
             where p.member.id = :memberId
             order by p.paidAt desc, p.id desc
            """)
    List<FundingParticipation> findDetailByMemberId(@Param("memberId") Long memberId);

    @Query("""
            select coalesce(sum(p.mockPaidAmount), 0)
              from FundingParticipation p
             where p.paymentStatus = :paymentStatus
            """)
    java.math.BigDecimal sumMockPaidAmountByPaymentStatus(
            @Param("paymentStatus") FundingPaymentStatus paymentStatus
    );
}
