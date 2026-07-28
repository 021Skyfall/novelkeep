package com.novelkeep.novel.repository;

import java.util.List;
import java.util.Optional;

import com.novelkeep.novel.domain.Novel;
import com.novelkeep.novel.domain.NovelStatus;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NovelRepository extends JpaRepository<Novel, Long> {

    @EntityGraph(attributePaths = {"author", "parts"})
    List<Novel> findAllByStatusNotOrderByUpdatedAtDesc(NovelStatus status);

    @EntityGraph(attributePaths = {"author", "parts"})
    List<Novel> findAllByAuthorIdOrderByUpdatedAtDesc(Long authorId);

    @Override
    @EntityGraph(attributePaths = {"author", "parts"})
    Optional<Novel> findById(Long id);

    @EntityGraph(attributePaths = {"author", "parts"})
    Optional<Novel> findByIdAndAuthorId(Long id, Long authorId);
}
