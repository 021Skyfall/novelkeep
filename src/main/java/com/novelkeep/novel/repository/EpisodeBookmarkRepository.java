package com.novelkeep.novel.repository;

import java.util.List;
import java.util.Optional;

import com.novelkeep.novel.domain.EpisodeBookmark;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpisodeBookmarkRepository extends JpaRepository<EpisodeBookmark, Long> {

    Optional<EpisodeBookmark> findByMemberIdAndNovelId(Long memberId, Long novelId);

    @EntityGraph(attributePaths = {"novel", "episode"})
    List<EpisodeBookmark> findAllByMemberIdOrderByUpdatedAtDesc(Long memberId);

    long countByMemberId(Long memberId);

    void deleteByMemberIdAndNovelId(Long memberId, Long novelId);

    void deleteByNovelId(Long novelId);

    void deleteByEpisodeId(Long episodeId);

    void deleteByEpisodeStoryPartId(Long storyPartId);
}
