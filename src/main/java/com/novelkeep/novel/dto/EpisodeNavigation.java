package com.novelkeep.novel.dto;

import com.novelkeep.novel.domain.Episode;
import com.novelkeep.novel.domain.StoryPart;

public class EpisodeNavigation {

    private final Episode previousEpisode;
    private final Episode nextEpisode;
    private final StoryPart previousPart;
    private final StoryPart nextPart;
    private final Episode previousPartEpisode;
    private final Episode nextPartEpisode;

    public EpisodeNavigation(
            Episode previousEpisode,
            Episode nextEpisode,
            StoryPart previousPart,
            StoryPart nextPart,
            Episode previousPartEpisode,
            Episode nextPartEpisode
    ) {
        this.previousEpisode = previousEpisode;
        this.nextEpisode = nextEpisode;
        this.previousPart = previousPart;
        this.nextPart = nextPart;
        this.previousPartEpisode = previousPartEpisode;
        this.nextPartEpisode = nextPartEpisode;
    }

    public Episode getPreviousEpisode() {
        return previousEpisode;
    }

    public Episode getNextEpisode() {
        return nextEpisode;
    }

    public StoryPart getPreviousPart() {
        return previousPart;
    }

    public StoryPart getNextPart() {
        return nextPart;
    }

    public Episode getPreviousPartEpisode() {
        return previousPartEpisode;
    }

    public Episode getNextPartEpisode() {
        return nextPartEpisode;
    }
}
