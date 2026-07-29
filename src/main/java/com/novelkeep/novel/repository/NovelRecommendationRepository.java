package com.novelkeep.novel.repository;

import java.util.Optional;

import com.novelkeep.novel.domain.NovelRecommendation;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NovelRecommendationRepository extends JpaRepository<NovelRecommendation, Long> {

    Optional<NovelRecommendation> findByMemberIdAndNovelId(Long memberId, Long novelId);

    boolean existsByMemberIdAndNovelId(Long memberId, Long novelId);

    void deleteByNovelId(Long novelId);
}
