package com.novelkeep.novel.repository;

import java.util.List;
import java.util.Optional;

import com.novelkeep.novel.domain.StoryPart;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoryPartRepository extends JpaRepository<StoryPart, Long> {

    @EntityGraph(attributePaths = {"novel", "novel.author", "episodes"})
    Optional<StoryPart> findById(Long id);

    @EntityGraph(attributePaths = {"episodes"})
    List<StoryPart> findByNovelIdOrderByPartNumberAsc(Long novelId);

    @Query("""
            select sp
              from StoryPart sp
              join fetch sp.novel n
              join fetch n.author
             where sp.id = :partId
               and n.author.id = :authorId
            """)
    Optional<StoryPart> findByIdAndAuthorId(
            @Param("partId") Long partId,
            @Param("authorId") Long authorId
    );

    @Query("select coalesce(max(sp.partNumber), 0) from StoryPart sp where sp.novel.id = :novelId")
    int findMaxPartNumberByNovelId(@Param("novelId") Long novelId);
}
