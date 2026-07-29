package com.novelkeep.novel.repository;

import java.util.List;
import java.util.Optional;

import com.novelkeep.novel.domain.Novel;
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

    @Query("""
            select distinct n.genre
              from Novel n
             where n.visibility = :visibility
             order by n.genre asc
            """)
    List<String> findDistinctGenresByVisibility(@Param("visibility") NovelVisibility visibility);

    @Query("""
            select distinct n.genre
              from Novel n
             where n.author.id = :authorId
             order by n.genre asc
            """)
    List<String> findDistinctGenresByAuthorId(@Param("authorId") Long authorId);

    @Query("select distinct n.genre from Novel n order by n.genre asc")
    List<String> findDistinctGenres();

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
}
