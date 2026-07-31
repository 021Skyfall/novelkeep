package com.novelkeep.novel.repository;

import java.util.List;
import java.util.Optional;

import com.novelkeep.novel.domain.Novel;
import com.novelkeep.novel.domain.NovelGenre;
import com.novelkeep.novel.domain.NovelStatus;
import com.novelkeep.novel.domain.NovelVisibility;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NovelRepository extends JpaRepository<Novel, Long>, JpaSpecificationExecutor<Novel> {

    @Override
    @EntityGraph(attributePaths = {"author", "parts"})
    Optional<Novel> findById(Long id);

    @EntityGraph(attributePaths = {"author", "parts"})
    Optional<Novel> findByIdAndAuthorId(Long id, Long authorId);

    @EntityGraph(attributePaths = {"author", "parts"})
    List<Novel> findByAuthorIdOrderByUpdatedAtDesc(Long authorId);

    @Query("""
            select distinct n
              from Novel n
              join fetch n.author
              left join fetch n.parts
             where n.id = :novelId
            """)
    Optional<Novel> findDetailById(@Param("novelId") Long novelId);

    @Query("""
            select distinct g
              from Novel n
              join n.genres g
             where n.visibility = :visibility
             order by g asc
            """)
    List<NovelGenre> findDistinctGenresByVisibility(@Param("visibility") NovelVisibility visibility);

    @Query("""
            select distinct g
              from Novel n
              join n.genres g
             where n.author.id = :authorId
             order by g asc
            """)
    List<NovelGenre> findDistinctGenresByAuthorId(@Param("authorId") Long authorId);

    @Query("select distinct g from Novel n join n.genres g order by g asc")
    List<NovelGenre> findDistinctGenres();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Novel n
               set n.recommendationCount = n.recommendationCount + 1
             where n.id = :novelId
            """)
    int increaseRecommendationCount(@Param("novelId") Long novelId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Novel n
               set n.recommendationCount = case
                     when n.recommendationCount > 0 then n.recommendationCount - 1
                     else 0
                   end
             where n.id = :novelId
            """)
    int decreaseRecommendationCount(@Param("novelId") Long novelId);

    @Query("""
            select n
              from Novel n
              join fetch n.author
             where n.visibility = :visibility
             order by n.recommendationCount desc, n.updatedAt desc
            """)
    List<Novel> findPublicOrderByPopularity(
            @Param("visibility") NovelVisibility visibility,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("""
            select n
              from Novel n
              join fetch n.author
             where n.visibility = :visibility
               and n.status = :status
             order by n.updatedAt desc
            """)
    List<Novel> findPublicByStatusOrderByUpdatedAtDesc(
            @Param("visibility") NovelVisibility visibility,
            @Param("status") NovelStatus status,
            org.springframework.data.domain.Pageable pageable
    );
}
