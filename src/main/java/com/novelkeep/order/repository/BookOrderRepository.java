package com.novelkeep.order.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.novelkeep.order.domain.BookOrder;
import com.novelkeep.order.domain.BookOrderStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookOrderRepository extends JpaRepository<BookOrder, Long> {

    @Query("""
            select distinct o
              from BookOrder o
              join fetch o.participation p
              join fetch p.campaign c
              join fetch c.storyPart sp
              join fetch sp.novel n
             where (:status is null or o.status = :status)
               and (:fromAt is null or o.orderedAt >= :fromAt)
               and (:toAt is null or o.orderedAt < :toAt)
               and (:novelTitle is null or :novelTitle = '' or lower(n.title) like lower(concat('%', :novelTitle, '%')))
             order by o.orderedAt desc, o.id desc
            """)
    List<BookOrder> search(
            @Param("novelTitle") String novelTitle,
            @Param("status") BookOrderStatus status,
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toAt") LocalDateTime toAt
    );

    @Query("""
            select o
              from BookOrder o
              join fetch o.participation p
              join fetch p.campaign
             where p.campaign.id in :campaignIds
            """)
    List<BookOrder> findByCampaignIdIn(@Param("campaignIds") Collection<Long> campaignIds);

    @Query("""
            select o
              from BookOrder o
              join fetch o.participation p
              join fetch p.campaign
             where p.campaign.id = :campaignId
               and p.member.id = :memberId
            """)
    Optional<BookOrder> findByCampaignIdAndMemberId(
            @Param("campaignId") Long campaignId,
            @Param("memberId") Long memberId
    );

    @Query("""
            select distinct p.campaign.id
              from BookOrder o
              join o.participation p
             where o.status = :status
               and p.campaign.id in :campaignIds
            """)
    List<Long> findCampaignIdsByStatusAndCampaignIdIn(
            @Param("status") BookOrderStatus status,
            @Param("campaignIds") Collection<Long> campaignIds
    );

    @Query("""
            select o
              from BookOrder o
              join fetch o.participation p
              join fetch p.member m
              join fetch p.campaign c
              join fetch c.storyPart sp
              join fetch sp.novel n
             where p.campaign.id = :campaignId
             order by o.id asc
            """)
    List<BookOrder> findDetailByCampaignId(@Param("campaignId") Long campaignId);

    @Query("""
            select o
              from BookOrder o
              join fetch o.participation p
              join fetch p.campaign c
              join fetch c.storyPart sp
              join fetch sp.novel n
             where o.id = :id
            """)
    Optional<BookOrder> findDetailById(@Param("id") Long id);

    @Query("""
            select distinct o
              from BookOrder o
              join fetch o.participation p
              join fetch p.campaign c
              join fetch c.storyPart sp
              join fetch sp.novel n
             where p.member.id = :memberId
             order by o.orderedAt desc, o.id desc
            """)
    List<BookOrder> findDetailByMemberId(@Param("memberId") Long memberId);

    long countByStatus(BookOrderStatus status);
}
