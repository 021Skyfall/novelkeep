package com.novelkeep.novel.repository;

import java.util.List;

import com.novelkeep.novel.domain.Episode;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EpisodeRepository extends JpaRepository<Episode, Long> {

    List<Episode> findByStoryPartIdOrderByEpisodeNumberAsc(Long storyPartId);
}
