package com.novelkeep.novel.repository;

import java.util.Optional;

import com.novelkeep.novel.domain.NovelFavorite;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NovelFavoriteRepository extends JpaRepository<NovelFavorite, Long> {

    Optional<NovelFavorite> findByMemberIdAndNovelId(Long memberId, Long novelId);

    boolean existsByMemberIdAndNovelId(Long memberId, Long novelId);

    void deleteByNovelId(Long novelId);
}
