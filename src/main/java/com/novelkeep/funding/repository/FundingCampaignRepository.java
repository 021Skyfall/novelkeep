package com.novelkeep.funding.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.novelkeep.funding.domain.FundingCampaign;
import com.novelkeep.funding.domain.FundingCampaignStatus;
import com.novelkeep.novel.domain.NovelVisibility;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FundingCampaignRepository extends JpaRepository<FundingCampaign, Long> {

    @Query("""
            select c from FundingCampaign c
            join fetch c.storyPart p
            join fetch p.novel n
            where c.status = :status
              and c.startAt <= :now
              and c.endAt >= :now
              and n.visibility = :visibility
            order by c.createdAt desc
            """)
    List<FundingCampaign> findOpenPublicCampaigns(
            @Param("status") FundingCampaignStatus status,
            @Param("now") LocalDateTime now,
            @Param("visibility") NovelVisibility visibility,
            Pageable pageable
    );

    @Query("""
            select c from FundingCampaign c
            join fetch c.storyPart p
            join fetch p.novel n
            where c.id = :id
            """)
    Optional<FundingCampaign> findDetailById(@Param("id") Long id);
}
