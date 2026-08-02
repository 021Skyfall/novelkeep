package com.novelkeep.home.service;

import java.time.LocalDateTime;
import java.util.List;

import com.novelkeep.funding.domain.FundingCampaign;
import com.novelkeep.funding.domain.FundingCampaignStatus;
import com.novelkeep.funding.repository.FundingCampaignRepository;
import com.novelkeep.novel.domain.Episode;
import com.novelkeep.novel.domain.EpisodeStatus;
import com.novelkeep.novel.domain.Novel;
import com.novelkeep.novel.domain.NovelStatus;
import com.novelkeep.novel.domain.NovelVisibility;
import com.novelkeep.novel.repository.EpisodeRepository;
import com.novelkeep.novel.repository.NovelRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomeShowcaseService {

    private static final int SHOWCASE_SIZE = 10;
    private static final int FUNDING_SHOWCASE_SIZE = 5;

    private final NovelRepository novelRepository;
    private final EpisodeRepository episodeRepository;
    private final FundingCampaignRepository fundingCampaignRepository;

    public HomeShowcaseService(
            NovelRepository novelRepository,
            EpisodeRepository episodeRepository,
            FundingCampaignRepository fundingCampaignRepository
    ) {
        this.novelRepository = novelRepository;
        this.episodeRepository = episodeRepository;
        this.fundingCampaignRepository = fundingCampaignRepository;
    }

    @Transactional(readOnly = true)
    public List<Novel> popularNovels() {
        List<Novel> novels = novelRepository.findPublicOrderByPopularity(
                NovelVisibility.PUBLIC,
                PageRequest.of(0, SHOWCASE_SIZE)
        );
        novels.forEach(this::initializeNovelCard);
        return novels;
    }

    @Transactional(readOnly = true)
    public List<Novel> completedNovels() {
        List<Novel> novels = novelRepository.findPublicByStatusOrderByUpdatedAtDesc(
                NovelVisibility.PUBLIC,
                NovelStatus.COMPLETED,
                PageRequest.of(0, SHOWCASE_SIZE)
        );
        novels.forEach(this::initializeNovelCard);
        return novels;
    }

    @Transactional(readOnly = true)
    public List<Episode> latestEpisodes() {
        return episodeRepository.findPublicPublishedOrderByLatest(
                EpisodeStatus.PUBLISHED,
                NovelVisibility.PUBLIC,
                PageRequest.of(0, SHOWCASE_SIZE)
        );
    }

    @Transactional(readOnly = true)
    public List<FundingCampaign> openFundings() {
        List<FundingCampaign> campaigns = fundingCampaignRepository.findOpenPublicCampaigns(
                FundingCampaignStatus.IN_PROGRESS,
                LocalDateTime.now(),
                NovelVisibility.PUBLIC,
                PageRequest.of(0, FUNDING_SHOWCASE_SIZE)
        );
        campaigns.forEach(campaign -> {
            campaign.getStoryPart().getNovel().getGenres().size();
            campaign.getStoryPart().getNovel().getParts().size();
        });
        return campaigns;
    }

    private void initializeNovelCard(Novel novel) {
        novel.getGenres().size();
        novel.getParts().size();
    }
}
