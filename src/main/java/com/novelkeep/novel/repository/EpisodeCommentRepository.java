package com.novelkeep.novel.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.novelkeep.novel.domain.EpisodeComment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EpisodeCommentRepository extends JpaRepository<EpisodeComment, Long> {

    @Query("""
            select distinct c from EpisodeComment c
            join fetch c.member
            left join fetch c.replies r
            left join fetch r.member
            where c.episode.id = :episodeId
              and c.parent is null
            order by c.createdAt asc
            """)
    List<EpisodeComment> findRootsWithRepliesByEpisodeId(@Param("episodeId") Long episodeId);

    @Query("""
            select c from EpisodeComment c
            join fetch c.member
            join fetch c.episode e
            left join fetch c.parent
            where c.id = :id
            """)
    Optional<EpisodeComment> findDetailById(@Param("id") Long id);

    @Query("""
            select c.episode.id, count(c)
            from EpisodeComment c
            where c.episode.id in :episodeIds
              and c.deletedAt is null
            group by c.episode.id
            """)
    List<Object[]> countGroupedByEpisodeIds(@Param("episodeIds") Collection<Long> episodeIds);
}
