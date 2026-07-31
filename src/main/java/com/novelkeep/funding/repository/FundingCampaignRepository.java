package com.novelkeep.funding.repository;

import java.time.LocalDateTime;
import java.util.Collection;
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

    @Query("""
            select c from FundingCampaign c
            join fetch c.storyPart p
            join fetch p.novel n
            where n.author.id = :authorId
            order by c.createdAt desc
            """)
    List<FundingCampaign> findByAuthorId(@Param("authorId") Long authorId);

    @Query("""
            select c from FundingCampaign c
            join fetch c.storyPart p
            join fetch p.novel n
            where c.status = :status
              and n.id in :novelIds
            order by c.createdAt desc
            """)
    List<FundingCampaign> findByStatusAndNovelIdIn(
            @Param("status") FundingCampaignStatus status,
            @Param("novelIds") Collection<Long> novelIds
    );

    @Query("""
            select c from FundingCampaign c
            join fetch c.storyPart p
            join fetch p.novel n
            where n.author.id = :authorId
              and c.status = :status
            order by c.createdAt desc
            """)
    List<FundingCampaign> findByAuthorIdAndStatus(
            @Param("authorId") Long authorId,
            @Param("status") FundingCampaignStatus status
    );

    @Query("""
            select c from FundingCampaign c
            join fetch c.storyPart p
            join fetch p.novel n
            where c.status = :status
              and p.id in :partIds
            order by c.createdAt desc
            """)
    List<FundingCampaign> findByStatusAndStoryPartIdIn(
            @Param("status") FundingCampaignStatus status,
            @Param("partIds") Collection<Long> partIds
    );

    boolean existsByStoryPartId(Long storyPartId);

    boolean existsByStoryPartNovelId(Long novelId);

    boolean existsByStoryPartIdAndStatus(Long storyPartId, FundingCampaignStatus status);

    @Query("""
            select c from FundingCampaign c
            join fetch c.storyPart p
            join fetch p.novel n
            where c.id = :id
              and n.author.id = :authorId
            """)
    Optional<FundingCampaign> findByIdAndAuthorId(@Param("id") Long id, @Param("authorId") Long authorId);
}
