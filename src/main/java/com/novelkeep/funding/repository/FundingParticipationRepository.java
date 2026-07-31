package com.novelkeep.funding.repository;

import com.novelkeep.funding.domain.FundingParticipation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FundingParticipationRepository extends JpaRepository<FundingParticipation, Long> {

    boolean existsByCampaignId(Long campaignId);
}
