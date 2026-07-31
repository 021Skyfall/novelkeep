package com.novelkeep.funding.service;

import com.novelkeep.funding.domain.FundingCampaign;
import com.novelkeep.funding.repository.FundingCampaignRepository;
import com.novelkeep.novel.domain.NovelVisibility;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FundingCampaignService {

    private final FundingCampaignRepository fundingCampaignRepository;

    public FundingCampaignService(FundingCampaignRepository fundingCampaignRepository) {
        this.fundingCampaignRepository = fundingCampaignRepository;
    }

    @Transactional(readOnly = true)
    public FundingCampaign findReadable(Long campaignId) {
        FundingCampaign campaign = fundingCampaignRepository.findDetailById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (campaign.getStoryPart().getNovel().getVisibility() != NovelVisibility.PUBLIC) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        campaign.getStoryPart().getNovel().getGenres().size();
        campaign.getStoryPart().getNovel().getParts().size();
        return campaign;
    }
}
