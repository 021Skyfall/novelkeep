package com.novelkeep.novel.repository;

import java.util.List;
import java.util.Optional;

import com.novelkeep.novel.domain.Episode;
import com.novelkeep.novel.domain.EpisodeStatus;
import com.novelkeep.novel.domain.NovelVisibility;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EpisodeRepository extends JpaRepository<Episode, Long> {

    @EntityGraph(attributePaths = {"storyPart", "storyPart.novel", "storyPart.novel.author"})
    Optional<Episode> findById(Long id);

    List<Episode> findByStoryPartIdOrderByEpisodeNumberAsc(Long storyPartId);

    List<Episode> findByStoryPartIdAndStatusOrderByEpisodeNumberAsc(
            Long storyPartId,
            EpisodeStatus status
    );

    @Query("""
            select e
              from Episode e
              join fetch e.storyPart sp
              join fetch sp.novel n
              join fetch n.author
             where e.id = :episodeId
               and n.author.id = :authorId
            """)
    Optional<Episode> findByIdAndAuthorId(
            @Param("episodeId") Long episodeId,
            @Param("authorId") Long authorId
    );

    @Query("select coalesce(max(e.episodeNumber), 0) from Episode e where e.storyPart.id = :storyPartId")
    int findMaxEpisodeNumberByStoryPartId(@Param("storyPartId") Long storyPartId);

    @Query("""
            select e
              from Episode e
              join fetch e.storyPart sp
              join fetch sp.novel n
             where e.status = :episodeStatus
               and n.visibility = :visibility
             order by coalesce(e.publishedAt, e.updatedAt) desc
            """)
    List<Episode> findPublicPublishedOrderByLatest(
            @Param("episodeStatus") EpisodeStatus episodeStatus,
            @Param("visibility") NovelVisibility visibility,
            org.springframework.data.domain.Pageable pageable
    );
}
