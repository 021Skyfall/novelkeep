package com.novelkeep.funding.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;

import com.novelkeep.funding.domain.FundingCampaign;
import com.novelkeep.funding.domain.FundingCampaignStatus;
import com.novelkeep.funding.domain.FundingGuide;
import com.novelkeep.funding.domain.FundingParticipation;
import com.novelkeep.funding.dto.WriterFundingForm;
import com.novelkeep.funding.repository.FundingCampaignRepository;
import com.novelkeep.funding.repository.FundingParticipationRepository;
import com.novelkeep.member.domain.Member;
import com.novelkeep.member.repository.MemberRepository;
import com.novelkeep.novel.domain.Novel;
import com.novelkeep.novel.domain.NovelVisibility;
import com.novelkeep.novel.domain.StoryPart;
import com.novelkeep.novel.domain.StoryPartStatus;
import com.novelkeep.novel.repository.NovelRepository;
import com.novelkeep.novel.repository.StoryPartRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FundingCampaignService {

    private static final int PARTICIPATION_QUANTITY = 1;

    private final FundingCampaignRepository fundingCampaignRepository;
    private final FundingParticipationRepository fundingParticipationRepository;
    private final NovelRepository novelRepository;
    private final StoryPartRepository storyPartRepository;
    private final MemberRepository memberRepository;

    public FundingCampaignService(
            FundingCampaignRepository fundingCampaignRepository,
            FundingParticipationRepository fundingParticipationRepository,
            NovelRepository novelRepository,
            StoryPartRepository storyPartRepository,
            MemberRepository memberRepository
    ) {
        this.fundingCampaignRepository = fundingCampaignRepository;
        this.fundingParticipationRepository = fundingParticipationRepository;
        this.novelRepository = novelRepository;
        this.storyPartRepository = storyPartRepository;
        this.memberRepository = memberRepository;
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

    @Transactional(readOnly = true)
    public List<Novel> findOwnedNovelsWithParts(Long memberId) {
        List<Novel> novels = novelRepository.findByAuthorIdOrderByUpdatedAtDesc(memberId);
        novels.forEach(novel -> novel.getParts().forEach(part -> part.getEpisodes().size()));
        return novels;
    }

    @Transactional(readOnly = true)
    public List<FundingCampaign> findOpenOwnedCampaigns(Long memberId) {
        List<FundingCampaign> campaigns = fundingCampaignRepository.findByAuthorIdAndStatus(
                memberId,
                FundingCampaignStatus.OPEN
        );
        campaigns.forEach(campaign -> {
            Novel novel = campaign.getStoryPart().getNovel();
            novel.getParts().size();
            novel.getGenres().size();
            campaign.getStoryPart().getEpisodes().size();
        });
        return campaigns;
    }

    @Transactional(readOnly = true)
    public Map<Long, List<FundingCampaign>> findOpenCampaignsGroupedByNovelId(Collection<Long> novelIds) {
        Map<Long, List<FundingCampaign>> result = new LinkedHashMap<>();
        if (novelIds == null || novelIds.isEmpty()) {
            return result;
        }
        List<FundingCampaign> campaigns = fundingCampaignRepository.findByStatusAndNovelIdIn(
                FundingCampaignStatus.OPEN,
                novelIds
        );
        for (FundingCampaign campaign : campaigns) {
            Novel novel = campaign.getStoryPart().getNovel();
            novel.getParts().size();
            result.computeIfAbsent(novel.getId(), id -> new ArrayList<>()).add(campaign);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<Long, FundingCampaign> findOpenCampaignsByPartIds(Collection<Long> partIds) {
        Map<Long, FundingCampaign> result = new LinkedHashMap<>();
        if (partIds == null || partIds.isEmpty()) {
            return result;
        }
        List<FundingCampaign> campaigns = fundingCampaignRepository.findByStatusAndStoryPartIdIn(
                FundingCampaignStatus.OPEN,
                partIds
        );
        for (FundingCampaign campaign : campaigns) {
            campaign.getStoryPart().getEpisodes().size();
            result.putIfAbsent(campaign.getStoryPart().getId(), campaign);
        }
        return result;
    }

    @Transactional
    public FundingCampaign startCampaign(Long memberId, WriterFundingForm form) {
        StoryPart part = requireFundablePart(form.getNovelId(), form.getPartId(), memberId);
        Novel novel = part.getNovel();
        if (novel.getVisibility() != NovelVisibility.PUBLIC) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "공개 작품에서만 펀딩을 시작할 수 있습니다.");
        }
        if (fundingCampaignRepository.existsByStoryPartIdAndStatus(part.getId(), FundingCampaignStatus.OPEN)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이 부에 이미 진행 중인 펀딩이 있습니다. 종료된 뒤에 다시 시작할 수 있습니다."
            );
        }
        try {
            FundingGuide.validateCreateSchedule(form.getStartAt(), form.getEndAt());
            FundingGuide.validateTarget(form.getTargetQuantity(), form.getPriceAmount());
            FundingCampaign campaign = FundingCampaign.open(
                    part,
                    form.getTargetQuantity(),
                    0,
                    form.getPriceAmount(),
                    form.getStartAt(),
                    form.getEndAt()
            );
            return fundingCampaignRepository.save(campaign);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @Transactional
    public Long createDraft(Long memberId, WriterFundingForm form) {
        return startCampaign(memberId, form).getId();
    }

    @Transactional
    public FundingCampaign updateOpenCampaign(Long campaignId, Long memberId, WriterFundingForm form) {
        FundingCampaign campaign = requireOwnedCampaign(campaignId, memberId);
        if (campaign.getStatus() != FundingCampaignStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "진행 중인 펀딩만 수정할 수 있습니다.");
        }
        try {
            campaign.updateWhileOpen(form.getTargetQuantity(), form.getPriceAmount(), form.getEndAt());
            return campaign;
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @Transactional
    public void updateCampaign(Long campaignId, Long memberId, WriterFundingForm form) {
        updateOpenCampaign(campaignId, memberId, form);
    }

    @Transactional
    public void openDraft(Long campaignId, Long memberId) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "펀딩은 내 작품의 각 부에서 바로 시작합니다.");
    }

    @Transactional
    public void cancel(Long campaignId, Long memberId) {
        FundingCampaign campaign = requireOwnedCampaign(campaignId, memberId);
        if (campaign.getStatus() != FundingCampaignStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "진행 중인 펀딩만 취소할 수 있습니다.");
        }
        if (campaign.getCurrentQuantity() > 0
                || fundingParticipationRepository.existsByCampaignId(campaignId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "참여(수요)가 1부 이상인 펀딩은 직접 취소할 수 없습니다. 담당자에게 문의해 주세요."
            );
        }
        fundingCampaignRepository.delete(campaign);
    }

    @Transactional(readOnly = true)
    public boolean hasParticipated(Long campaignId, Long memberId) {
        if (campaignId == null || memberId == null) {
            return false;
        }
        return fundingParticipationRepository.existsByCampaignIdAndMemberId(campaignId, memberId);
    }

    @Transactional(readOnly = true)
    public Set<Long> findParticipatedCampaignIds(Long memberId, Collection<Long> campaignIds) {
        if (memberId == null || campaignIds == null || campaignIds.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(
                fundingParticipationRepository.findCampaignIdsByMemberIdAndCampaignIdIn(memberId, campaignIds)
        );
    }

    @Transactional
    public FundingCampaign participate(Long campaignId, Long memberId) {
        if (memberId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "체험 역할을 선택해 주세요.");
        }
        FundingCampaign campaign = fundingCampaignRepository.findDetailById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Novel novel = campaign.getStoryPart().getNovel();
        if (novel.getVisibility() != NovelVisibility.PUBLIC) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        LocalDateTime now = FundingGuide.nowKorea();
        if (campaign.getStatus() != FundingCampaignStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "진행 중인 펀딩에만 참여할 수 있습니다.");
        }
        if (!campaign.isWithinPeriod(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "펀딩 기간이 아니어서 참여할 수 없습니다.");
        }
        if (novel.getAuthor().getId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인 작품의 펀딩에는 참여할 수 없습니다.");
        }
        if (fundingParticipationRepository.existsByCampaignIdAndMemberId(campaignId, memberId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 이 펀딩에 참여했습니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "체험 역할을 선택해 주세요."));
        try {
            FundingParticipation participation = FundingParticipation.paid(
                    campaign,
                    member,
                    PARTICIPATION_QUANTITY,
                    now
            );
            fundingParticipationRepository.save(participation);
            campaign.recordParticipation(PARTICIPATION_QUANTITY);
            return campaign;
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    public boolean isPartFundable(StoryPart part) {
        if (part == null || part.getStatus() == StoryPartStatus.UNPUBLISHED) {
            return false;
        }
        return part.allEpisodesPublished();
    }

    private StoryPart requireFundablePart(Long novelId, Long partId, Long memberId) {
        if (novelId == null || partId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "작품과 대상 부를 선택해 주세요.");
        }
        StoryPart part = storyPartRepository.findByIdAndAuthorId(partId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!part.getNovel().getId().equals(novelId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        assertPartFundable(part);
        return part;
    }

    private void assertPartFundable(StoryPart part) {
        if (part.getStatus() == StoryPartStatus.UNPUBLISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "미공개 부에는 펀딩을 시작할 수 없습니다.");
        }
        if (!part.hasEpisodes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "회차가 없는 부에는 펀딩을 시작할 수 없습니다.");
        }
        if (!part.allEpisodesPublished()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "미공개 회차가 있으면 펀딩을 시작할 수 없습니다. 모든 회차를 공개해 주세요."
            );
        }
    }

    private FundingCampaign requireOwnedCampaign(Long campaignId, Long memberId) {
        return fundingCampaignRepository.findByIdAndAuthorId(campaignId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
