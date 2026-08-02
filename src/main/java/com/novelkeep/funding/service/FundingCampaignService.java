package com.novelkeep.funding.service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;

import com.novelkeep.common.ExportText;
import com.novelkeep.funding.domain.FundingCampaign;
import com.novelkeep.funding.domain.FundingCampaignStatus;
import com.novelkeep.funding.domain.FundingGuide;
import com.novelkeep.funding.domain.FundingParticipation;
import com.novelkeep.funding.domain.FundingPaymentStatus;
import com.novelkeep.funding.dto.AdminFundingSearchCriteria;
import com.novelkeep.funding.dto.FundingApproveResult;
import com.novelkeep.funding.dto.FundingCloseResult;
import com.novelkeep.funding.dto.WriterFundingForm;
import com.novelkeep.funding.dto.WriterFundingSearchCriteria;
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
import com.novelkeep.order.domain.BookOrder;
import com.novelkeep.order.domain.BookOrderStatus;
import com.novelkeep.order.repository.BookOrderRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FundingCampaignService {

    private static final int MAX_PARTICIPATION_QUANTITY = 99;

    private final FundingCampaignRepository fundingCampaignRepository;
    private final FundingParticipationRepository fundingParticipationRepository;
    private final NovelRepository novelRepository;
    private final StoryPartRepository storyPartRepository;
    private final MemberRepository memberRepository;
    private final BookOrderRepository bookOrderRepository;

    public FundingCampaignService(
            FundingCampaignRepository fundingCampaignRepository,
            FundingParticipationRepository fundingParticipationRepository,
            NovelRepository novelRepository,
            StoryPartRepository storyPartRepository,
            MemberRepository memberRepository,
            BookOrderRepository bookOrderRepository
    ) {
        this.fundingCampaignRepository = fundingCampaignRepository;
        this.fundingParticipationRepository = fundingParticipationRepository;
        this.novelRepository = novelRepository;
        this.storyPartRepository = storyPartRepository;
        this.memberRepository = memberRepository;
        this.bookOrderRepository = bookOrderRepository;
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
    public List<FundingCampaign> findOwnedCampaigns(Long memberId, WriterFundingSearchCriteria criteria) {
        List<FundingCampaign> campaigns = fundingCampaignRepository.findByAuthorId(memberId);
        EnumSet<FundingCampaignStatus> managed = EnumSet.of(
                FundingCampaignStatus.OPEN,
                FundingCampaignStatus.SUCCESS,
                FundingCampaignStatus.FAILED,
                FundingCampaignStatus.CANCELLED
        );
        String titleKeyword = criteria != null && criteria.getNovelTitle() != null
                ? criteria.getNovelTitle().trim().toLowerCase(Locale.ROOT)
                : "";
        FundingCampaignStatus statusFilter = criteria != null ? criteria.getStatus() : null;
        BookOrderStatus orderStatusFilter = criteria != null ? criteria.getOrderStatus() : null;
        WriterFundingSearchCriteria.SortField sortField = criteria != null ? criteria.getSortField() : null;
        WriterFundingSearchCriteria.SortDir sortDir = criteria != null ? criteria.getSortDir() : null;

        List<FundingCampaign> filtered = new ArrayList<>();
        for (FundingCampaign campaign : campaigns) {
            if (!managed.contains(campaign.getStatus())) {
                continue;
            }
            if (statusFilter != null && campaign.getStatus() != statusFilter) {
                continue;
            }
            Novel novel = campaign.getStoryPart().getNovel();
            if (!titleKeyword.isEmpty()) {
                String title = novel.getTitle() == null ? "" : novel.getTitle().toLowerCase(Locale.ROOT);
                if (!title.contains(titleKeyword)) {
                    continue;
                }
            }
            novel.getParts().size();
            novel.getGenres().size();
            campaign.getStoryPart().getEpisodes().size();
            filtered.add(campaign);
        }

        if (orderStatusFilter != null && !filtered.isEmpty()) {
            List<Long> campaignIds = filtered.stream().map(FundingCampaign::getId).toList();
            Set<Long> matched = new LinkedHashSet<>(
                    bookOrderRepository.findCampaignIdsByStatusAndCampaignIdIn(orderStatusFilter, campaignIds)
            );
            filtered.removeIf(campaign -> !matched.contains(campaign.getId()));
        }

        filtered.sort(resolveOwnedComparator(sortField, sortDir));
        return filtered;
    }

    @Transactional(readOnly = true)
    public Map<Long, BookOrderStatus> resolveLeastOrderStatusByCampaignId(Collection<FundingCampaign> campaigns) {
        Map<Long, BookOrderStatus> result = new LinkedHashMap<>();
        if (campaigns == null || campaigns.isEmpty()) {
            return result;
        }
        List<Long> campaignIds = campaigns.stream().map(FundingCampaign::getId).toList();
        for (BookOrder order : bookOrderRepository.findByCampaignIdIn(campaignIds)) {
            Long campaignId = order.getParticipation().getCampaign().getId();
            BookOrderStatus current = result.get(campaignId);
            if (current == null || order.getStatus().ordinal() < current.ordinal()) {
                result.put(campaignId, order.getStatus());
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public BookOrderStatus findMemberOrderStatus(Long campaignId, Long memberId) {
        if (campaignId == null || memberId == null) {
            return null;
        }
        return bookOrderRepository.findByCampaignIdAndMemberId(campaignId, memberId)
                .map(BookOrder::getStatus)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<FundingCampaign> findAdminCampaigns(AdminFundingSearchCriteria criteria) {
        List<FundingCampaign> campaigns = fundingCampaignRepository.findByStatusIn(
                EnumSet.of(FundingCampaignStatus.SUCCESS, FundingCampaignStatus.FAILED)
        );
        String titleKeyword = criteria != null && criteria.getNovelTitle() != null
                ? criteria.getNovelTitle().trim().toLowerCase(Locale.ROOT)
                : "";
        AdminFundingSearchCriteria.ApprovalFilter approval = criteria != null
                ? criteria.getApproval()
                : AdminFundingSearchCriteria.ApprovalFilter.AWAITING;
        FundingCampaignStatus statusFilter = criteria != null ? criteria.getStatus() : null;
        AdminFundingSearchCriteria.SortField sortField = criteria != null ? criteria.getSortField() : null;
        AdminFundingSearchCriteria.SortDir sortDir = criteria != null ? criteria.getSortDir() : null;

        List<FundingCampaign> filtered = new ArrayList<>();
        for (FundingCampaign campaign : campaigns) {
            if (statusFilter != null && campaign.getStatus() != statusFilter) {
                continue;
            }
            if (approval == AdminFundingSearchCriteria.ApprovalFilter.AWAITING && !campaign.isAwaitingApproval()) {
                continue;
            }
            if (approval == AdminFundingSearchCriteria.ApprovalFilter.APPROVED && !campaign.isApproved()) {
                continue;
            }
            if (criteria != null && criteria.getClosedFrom() != null) {
                LocalDateTime closedAt = campaign.getClosedAt();
                if (closedAt == null || closedAt.toLocalDate().isBefore(criteria.getClosedFrom())) {
                    continue;
                }
            }
            if (criteria != null && criteria.getClosedTo() != null) {
                LocalDateTime closedAt = campaign.getClosedAt();
                if (closedAt == null || closedAt.toLocalDate().isAfter(criteria.getClosedTo())) {
                    continue;
                }
            }
            Novel novel = campaign.getStoryPart().getNovel();
            if (!titleKeyword.isEmpty()) {
                String title = novel.getTitle() == null ? "" : novel.getTitle().toLowerCase(Locale.ROOT);
                if (!title.contains(titleKeyword)) {
                    continue;
                }
            }
            novel.getParts().size();
            novel.getGenres().size();
            campaign.getStoryPart().getEpisodes().size();
            filtered.add(campaign);
        }
        filtered.sort(resolveAdminComparator(sortField, sortDir));
        return filtered;
    }

    @Transactional(readOnly = true)
    public byte[] exportAdminCsv(AdminFundingSearchCriteria criteria) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        StringBuilder sb = new StringBuilder();
        sb.append('\ufeff');
        sb.append("캠페인ID,작품명,권,판정,승인여부,현재부수,목표부수,판매가,종료일시\n");
        for (FundingCampaign campaign : findAdminCampaigns(criteria)) {
            Novel novel = campaign.getStoryPart().getNovel();
            StoryPart part = campaign.getStoryPart();
            String partLabel = novel.isMultiPart()
                    ? part.getPartNumber() + "부 · " + part.getTitle()
                    : "본편";
            sb.append(campaign.getId()).append(',')
                    .append(ExportText.csv(novel.getTitle())).append(',')
                    .append(ExportText.csv(partLabel)).append(',')
                    .append(ExportText.csv(campaign.getStatus().getDisplayName())).append(',')
                    .append(campaign.isApproved() ? "승인" : "대기").append(',')
                    .append(campaign.getCurrentQuantity()).append(',')
                    .append(campaign.getTargetQuantity()).append(',')
                    .append(campaign.getPriceAmount()).append(',')
                    .append(ExportText.csv(formatter.format(campaign.getEndAt())))
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public byte[] exportAdminJson(AdminFundingSearchCriteria criteria) {
        DateTimeFormatter iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        List<FundingCampaign> campaigns = findAdminCampaigns(criteria);
        for (int i = 0; i < campaigns.size(); i++) {
            FundingCampaign campaign = campaigns.get(i);
            Novel novel = campaign.getStoryPart().getNovel();
            StoryPart part = campaign.getStoryPart();
            String partLabel = novel.isMultiPart()
                    ? part.getPartNumber() + "부 · " + part.getTitle()
                    : "본편";
            if (i > 0) {
                sb.append(",\n");
            }
            sb.append("  {\n")
                    .append("    \"campaignId\": ").append(campaign.getId()).append(",\n")
                    .append("    \"novelTitle\": ").append(ExportText.jsonString(novel.getTitle())).append(",\n")
                    .append("    \"partLabel\": ").append(ExportText.jsonString(partLabel)).append(",\n")
                    .append("    \"status\": ").append(ExportText.jsonString(campaign.getStatus().name())).append(",\n")
                    .append("    \"statusLabel\": ").append(ExportText.jsonString(campaign.getStatus().getDisplayName())).append(",\n")
                    .append("    \"approved\": ").append(campaign.isApproved()).append(",\n")
                    .append("    \"currentQuantity\": ").append(campaign.getCurrentQuantity()).append(",\n")
                    .append("    \"targetQuantity\": ").append(campaign.getTargetQuantity()).append(",\n")
                    .append("    \"priceAmount\": ").append(campaign.getPriceAmount()).append(",\n")
                    .append("    \"endAt\": ").append(ExportText.jsonString(iso.format(campaign.getEndAt()))).append("\n")
                    .append("  }");
        }
        sb.append("\n]\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
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
    public void cancel(Long campaignId, Long memberId) {
        FundingCampaign campaign = requireOwnedCampaign(campaignId, memberId);
        if (campaign.getStatus() != FundingCampaignStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "진행 중인 펀딩만 취소할 수 있습니다.");
        }
        List<FundingParticipation> participations = fundingParticipationRepository.findByCampaignId(campaignId);
        boolean hasPaid = participations.stream()
                .anyMatch(p -> p.getPaymentStatus() == FundingPaymentStatus.PAID_MOCK);
        if (campaign.getCurrentQuantity() > 0 || hasPaid) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "참여(수요)가 1부 이상인 펀딩은 직접 취소할 수 없습니다. 담당자에게 문의해 주세요."
            );
        }
        try {
            campaign.markCancelled();
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public boolean hasParticipated(Long campaignId, Long memberId) {
        if (campaignId == null || memberId == null) {
            return false;
        }
        return fundingParticipationRepository.existsByCampaignIdAndMemberId(campaignId, memberId);
    }

    @Transactional(readOnly = true)
    public boolean hasPaidParticipation(Long campaignId, Long memberId) {
        if (campaignId == null || memberId == null) {
            return false;
        }
        return fundingParticipationRepository.findByCampaignIdAndMemberId(campaignId, memberId)
                .filter(p -> p.getPaymentStatus() == FundingPaymentStatus.PAID_MOCK)
                .isPresent();
    }

    @Transactional(readOnly = true)
    public int findPaidQuantity(Long campaignId, Long memberId) {
        if (campaignId == null || memberId == null) {
            return 0;
        }
        return fundingParticipationRepository.findByCampaignIdAndMemberId(campaignId, memberId)
                .filter(p -> p.getPaymentStatus() == FundingPaymentStatus.PAID_MOCK)
                .map(FundingParticipation::getQuantity)
                .orElse(0);
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
        return participate(campaignId, memberId, 1);
    }

    @Transactional
    public FundingCampaign participate(Long campaignId, Long memberId, int quantity) {
        if (memberId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "체험 역할을 선택해 주세요.");
        }
        if (quantity < 1 || quantity > MAX_PARTICIPATION_QUANTITY) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "참여 수량은 1~" + MAX_PARTICIPATION_QUANTITY + "부만 가능합니다."
            );
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
                    quantity,
                    now
            );
            fundingParticipationRepository.save(participation);
            campaign.recordParticipation(quantity);
            return campaign;
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @Transactional
    public FundingCloseResult closeCampaign(Long campaignId, Long memberId) {
        FundingCampaign campaign = requireOwnedCampaign(campaignId, memberId);
        if (campaign.getStatus() != FundingCampaignStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "진행 중인 펀딩만 마감할 수 있습니다.");
        }
        if (!campaign.canClose()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "수요가 0부인 펀딩은 마감할 수 없습니다. 취소를 이용해 주세요."
            );
        }

        List<FundingParticipation> paid = fundingParticipationRepository.findByCampaignIdAndPaymentStatus(
                campaignId,
                FundingPaymentStatus.PAID_MOCK
        );
        int paidCount = paid.stream().mapToInt(FundingParticipation::getQuantity).sum();
        StoryPart part = campaign.getStoryPart();
        boolean goalMet = campaign.isGoalMet();
        boolean contentReady = part.allEpisodesPublished()
                && part.getStatus() == StoryPartStatus.COMPLETED;

        try {
            if (goalMet) {
                if (!contentReady) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "성공 마감하려면 해당 부의 전체 회차 공개와 부 완결이 필요합니다."
                    );
                }
                campaign.closeAsSuccess();
                return new FundingCloseResult(true, paidCount, campaign);
            }
            campaign.closeAsFailed();
            return new FundingCloseResult(false, paidCount, campaign);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    /**
     * 진행 중(OPEN) 펀딩 참여 취소·환불. 성공 마감 이후에는 불가.
     */
    @Transactional
    public FundingCampaign cancelParticipation(Long campaignId, Long memberId) {
        if (memberId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "체험 역할을 선택해 주세요.");
        }
        FundingCampaign campaign = fundingCampaignRepository.findDetailById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (campaign.getStatus() != FundingCampaignStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "진행 중인 펀딩에서만 환불할 수 있습니다.");
        }
        FundingParticipation participation = fundingParticipationRepository
                .findByCampaignIdAndMemberId(campaignId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "참여 내역이 없습니다."));
        if (participation.getPaymentStatus() != FundingPaymentStatus.PAID_MOCK) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 환불된 참여입니다.");
        }
        LocalDateTime now = FundingGuide.nowKorea();
        try {
            participation.refundMock(now);
            campaign.withdrawParticipation(participation.getQuantity());
            return campaign;
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @Transactional
    public FundingApproveResult approveCampaign(Long campaignId) {
        FundingCampaign campaign = fundingCampaignRepository.findDetailById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!campaign.isAwaitingApproval()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "승인 대기 중인 펀딩만 처리할 수 있습니다.");
        }

        LocalDateTime now = FundingGuide.nowKorea();
        List<FundingParticipation> paid = fundingParticipationRepository.findByCampaignIdAndPaymentStatus(
                campaignId,
                FundingPaymentStatus.PAID_MOCK
        );

        try {
            if (campaign.getStatus() == FundingCampaignStatus.SUCCESS) {
                List<BookOrder> orders = new ArrayList<>(paid.size());
                int totalQuantity = 0;
                for (FundingParticipation participation : paid) {
                    orders.add(BookOrder.fromParticipation(participation, BookOrderStatus.PENDING, now));
                    totalQuantity += participation.getQuantity();
                }
                if (!orders.isEmpty()) {
                    bookOrderRepository.saveAll(orders);
                }
                campaign.markApproved(now);
                return new FundingApproveResult(true, orders.size(), totalQuantity, campaign);
            }

            int totalQuantity = 0;
            for (FundingParticipation participation : paid) {
                participation.refundMock(now);
                totalQuantity += participation.getQuantity();
            }
            campaign.markApproved(now);
            return new FundingApproveResult(false, paid.size(), totalQuantity, campaign);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @Transactional
    public FundingCampaign rejectCampaign(Long campaignId) {
        FundingCampaign campaign = fundingCampaignRepository.findDetailById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            campaign.reopenAfterReject();
            return campaign;
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    private Comparator<FundingCampaign> resolveOwnedComparator(
            WriterFundingSearchCriteria.SortField sortField,
            WriterFundingSearchCriteria.SortDir sortDir
    ) {
        Comparator<FundingCampaign> base = Comparator.comparing(
                c -> toMinute(c.getUpdatedAt()),
                Comparator.nullsLast(Comparator.reverseOrder())
        );
        if (sortField == null || sortDir == null) {
            return base;
        }
        Comparator<FundingCampaign> byField = switch (sortField) {
            case END -> Comparator.comparing(c -> toMinute(c.getEndAt()), Comparator.nullsLast(Comparator.naturalOrder()));
            case GAUGE -> Comparator.comparingInt(FundingCampaign::achievementPercent);
            case TARGET -> Comparator.comparingInt(FundingCampaign::getTargetQuantity);
            case PRICE -> Comparator.comparing(FundingCampaign::getPriceAmount);
            case UPDATED -> Comparator.comparing(c -> toMinute(c.getUpdatedAt()), Comparator.nullsLast(Comparator.naturalOrder()));
        };
        if (sortDir == WriterFundingSearchCriteria.SortDir.DESC) {
            byField = byField.reversed();
        }
        return byField.thenComparing(base);
    }

    private Comparator<FundingCampaign> resolveAdminComparator(
            AdminFundingSearchCriteria.SortField sortField,
            AdminFundingSearchCriteria.SortDir sortDir
    ) {
        Comparator<FundingCampaign> byClosedDesc = Comparator.comparing(
                c -> toMinute(c.getClosedAt()),
                Comparator.nullsLast(Comparator.reverseOrder())
        );
        if (sortField == null || sortDir == null) {
            return byClosedDesc;
        }
        Comparator<FundingCampaign> byField = switch (sortField) {
            case CLOSED -> Comparator.comparing(
                    c -> toMinute(c.getClosedAt()),
                    Comparator.nullsLast(Comparator.naturalOrder())
            );
            case GAUGE -> Comparator.comparingInt(FundingCampaign::achievementPercent);
            case PRICE -> Comparator.comparing(FundingCampaign::getPriceAmount);
            case TARGET -> Comparator.comparingInt(FundingCampaign::getTargetQuantity);
            case END -> Comparator.comparing(c -> toMinute(c.getEndAt()), Comparator.nullsLast(Comparator.naturalOrder()));
        };
        if (sortDir == AdminFundingSearchCriteria.SortDir.DESC) {
            byField = byField.reversed();
        }
        return byField.thenComparing(byClosedDesc);
    }

    private static LocalDateTime toMinute(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.withSecond(0).withNano(0);
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비공개 부에는 펀딩을 시작할 수 없습니다.");
        }
        if (!part.hasEpisodes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "회차가 없는 부에는 펀딩을 시작할 수 없습니다.");
        }
        if (!part.hasPublishedEpisode()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "공개 회차가 하나 이상 있어야 펀딩을 시작할 수 있습니다."
            );
        }
    }

    private FundingCampaign requireOwnedCampaign(Long campaignId, Long memberId) {
        return fundingCampaignRepository.findByIdAndAuthorId(campaignId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
