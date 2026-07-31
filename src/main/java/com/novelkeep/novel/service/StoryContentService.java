package com.novelkeep.novel.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.novelkeep.home.domain.ExperienceRole;
import com.novelkeep.novel.domain.Episode;
import com.novelkeep.novel.domain.EpisodeStatus;
import com.novelkeep.novel.domain.Novel;
import com.novelkeep.novel.domain.NovelStatus;
import com.novelkeep.novel.domain.StoryPart;
import com.novelkeep.novel.domain.StoryPartStatus;
import com.novelkeep.novel.dto.EpisodeForm;
import com.novelkeep.novel.dto.EpisodeNavigation;
import com.novelkeep.novel.dto.StoryPartForm;
import com.novelkeep.novel.repository.EpisodeBookmarkRepository;
import com.novelkeep.novel.repository.EpisodeRepository;
import com.novelkeep.novel.repository.NovelRepository;
import com.novelkeep.novel.repository.StoryPartRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StoryContentService {

    private final NovelRepository novelRepository;
    private final StoryPartRepository storyPartRepository;
    private final EpisodeRepository episodeRepository;
    private final EpisodeBookmarkRepository bookmarkRepository;

    public StoryContentService(
            NovelRepository novelRepository,
            StoryPartRepository storyPartRepository,
            EpisodeRepository episodeRepository,
            EpisodeBookmarkRepository bookmarkRepository
    ) {
        this.novelRepository = novelRepository;
        this.storyPartRepository = storyPartRepository;
        this.episodeRepository = episodeRepository;
        this.bookmarkRepository = bookmarkRepository;
    }

    @Transactional(readOnly = true)
    public Novel findOwnedNovelWithContents(Long novelId, Long memberId) {
        Novel novel = novelRepository.findByIdAndAuthorId(novelId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        initializeEpisodes(novel);
        return novel;
    }

    @Transactional(readOnly = true)
    public Novel findReadableNovelWithContents(Long novelId, Long memberId, ExperienceRole role) {
        Novel novel = novelRepository.findDetailById(novelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean admin = role == ExperienceRole.ADMIN;
        if (!novel.isReadableBy(memberId, admin)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        initializeEpisodes(novel);
        return novel;
    }

    @Transactional(readOnly = true)
    public StoryPart findOwnedPart(Long partId, Long memberId) {
        StoryPart part = storyPartRepository.findByIdAndAuthorId(partId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        part.getEpisodes().size();
        part.getNovel().getParts().size();
        return part;
    }

    @Transactional(readOnly = true)
    public Episode findOwnedEpisode(Long episodeId, Long memberId) {
        Episode episode = episodeRepository.findByIdAndAuthorId(episodeId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        episode.getStoryPart().getNovel().getParts().size();
        return episode;
    }

    @Transactional(readOnly = true)
    public Episode findReadableEpisode(Long episodeId, Long memberId, ExperienceRole role) {
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Novel novel = episode.getStoryPart().getNovel();
        boolean admin = role == ExperienceRole.ADMIN;
        boolean owner = novel.isOwnedBy(memberId);
        boolean privileged = owner || admin;
        if (!novel.isReadableBy(memberId, admin)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!privileged && episode.getStoryPart().getStatus() == StoryPartStatus.UNPUBLISHED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (episode.getStatus() != EpisodeStatus.PUBLISHED && !privileged) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        novel.getParts().size();
        return episode;
    }

    @Transactional(readOnly = true)
    public EpisodeNavigation buildNavigation(Long episodeId, Long memberId, ExperienceRole role) {
        Episode episode = findReadableEpisode(episodeId, memberId, role);
        Novel novel = findReadableNovelWithContents(
                episode.getStoryPart().getNovel().getId(),
                memberId,
                role
        );
        Long currentPartId = episode.getStoryPart().getId();
        boolean privileged = novel.isOwnedBy(memberId) || role == ExperienceRole.ADMIN;
        List<StoryPart> parts = readableParts(novel, privileged);
        StoryPart currentPart = parts.stream()
                .filter(part -> part.getId().equals(currentPartId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        List<Episode> currentEpisodes = readableEpisodes(currentPart, privileged);
        int index = indexOf(currentEpisodes, episodeId);

        Episode previousEpisode = index > 0 ? currentEpisodes.get(index - 1) : null;
        Episode nextEpisode = index >= 0 && index < currentEpisodes.size() - 1
                ? currentEpisodes.get(index + 1)
                : null;

        int partIndex = indexOfPart(parts, currentPartId);
        StoryPart previousPart = null;
        StoryPart nextPart = null;
        Episode previousPartEpisode = null;
        Episode nextPartEpisode = null;

        if (previousEpisode == null && partIndex > 0) {
            previousPart = parts.get(partIndex - 1);
            List<Episode> previousPartEpisodes = readableEpisodes(previousPart, privileged);
            if (!previousPartEpisodes.isEmpty()) {
                previousPartEpisode = previousPartEpisodes.getLast();
            }
        }
        if (nextEpisode == null && partIndex >= 0 && partIndex < parts.size() - 1) {
            nextPart = parts.get(partIndex + 1);
            List<Episode> nextPartEpisodes = readableEpisodes(nextPart, privileged);
            if (!nextPartEpisodes.isEmpty()) {
                nextPartEpisode = nextPartEpisodes.getFirst();
            }
        }

        return new EpisodeNavigation(
                previousEpisode,
                nextEpisode,
                previousPart,
                nextPart,
                previousPartEpisode,
                nextPartEpisode
        );
    }

    @Transactional
    public Long createPart(Long novelId, Long memberId, StoryPartForm form) {
        Novel novel = findOwnedNovelWithContents(novelId, memberId);
        int nextNumber = storyPartRepository.findMaxPartNumberByNovelId(novelId) + 1;
        StoryPartStatus status = form.getStatus() == null
                ? StoryPartStatus.SERIALIZING
                : form.getStatus();
        if (status == StoryPartStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "회차가 없는 새 부는 완결로 만들 수 없습니다.");
        }

        StoryPart part = StoryPart.create(nextNumber, normalize(form.getTitle()), status);
        novel.addPart(part);
        novelRepository.save(novel);
        return part.getId();
    }

    @Transactional
    public void updatePart(Long partId, Long memberId, StoryPartForm form) {
        StoryPart part = findOwnedPart(partId, memberId);
        Novel novel = part.getNovel();
        if (!novel.isMultiPart()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "마지막 권(부)은 제목과 상태를 별도로 수정할 수 없습니다.");
        }

        StoryPartStatus status = form.getStatus();
        if (status == StoryPartStatus.COMPLETED && !part.allEpisodesPublished()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "부 완결은 회차가 있고 모두 공개된 경우에만 가능합니다."
            );
        }

        StoryPartStatus previous = part.getStatus();
        part.update(normalize(form.getTitle()), status);
        if (previous == StoryPartStatus.COMPLETED && status != StoryPartStatus.COMPLETED
                && novel.getStatus() == NovelStatus.COMPLETED) {
            novel.changeStatus(NovelStatus.SERIALIZING);
        }
    }

    @Transactional
    public Long deletePart(Long partId, Long memberId) {
        StoryPart part = findOwnedPart(partId, memberId);
        Novel novel = part.getNovel();
        if (novel.getParts().size() <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "작품에는 최소 1개의 부가 필요합니다.");
        }
        if (novel.getParts().size() == 2
                && novel.getParts().stream()
                        .filter(item -> item.getPartNumber() >= 2)
                        .anyMatch(item -> !item.getEpisodes().isEmpty())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "추가 권에 회차가 있으면 삭제할 수 없습니다. 회차를 먼저 정리해 주세요."
            );
        }

        Long novelId = novel.getId();
        bookmarkRepository.deleteByEpisodeStoryPartId(partId);
        novel.removePart(part);
        renumberParts(novel);
        if (novel.getStatus() == NovelStatus.COMPLETED
                && novel.getParts().stream().anyMatch(item -> item.getStatus() != StoryPartStatus.COMPLETED)) {
            novel.changeStatus(NovelStatus.SERIALIZING);
        }
        return novelId;
    }

    @Transactional
    public Long createEpisode(Long partId, Long memberId, EpisodeForm form) {
        StoryPart part = findOwnedPart(partId, memberId);
        int nextNumber = episodeRepository.findMaxEpisodeNumberByStoryPartId(partId) + 1;
        Episode episode = Episode.create(
                nextNumber,
                normalize(form.getTitle()),
                form.getContent() == null ? "" : form.getContent(),
                form.getStatus()
        );
        part.addEpisode(episode);
        if (form.getStatus() != EpisodeStatus.PUBLISHED && part.getStatus() == StoryPartStatus.COMPLETED) {
            part.update(part.getTitle(), StoryPartStatus.SERIALIZING);
            if (part.getNovel().getStatus() == NovelStatus.COMPLETED) {
                part.getNovel().changeStatus(NovelStatus.SERIALIZING);
            }
        }
        storyPartRepository.save(part);
        return episode.getId();
    }

    @Transactional
    public void updateEpisode(Long episodeId, Long memberId, EpisodeForm form) {
        Episode episode = findOwnedEpisode(episodeId, memberId);
        StoryPart part = episode.getStoryPart();
        episode.update(
                normalize(form.getTitle()),
                form.getContent() == null ? "" : form.getContent(),
                form.getStatus()
        );

        if (form.getStatus() != EpisodeStatus.PUBLISHED && part.getStatus() == StoryPartStatus.COMPLETED) {
            part.update(part.getTitle(), StoryPartStatus.SERIALIZING);
            if (part.getNovel().getStatus() == NovelStatus.COMPLETED) {
                part.getNovel().changeStatus(NovelStatus.SERIALIZING);
            }
        }
    }

    @Transactional
    public Long deleteEpisode(Long episodeId, Long memberId) {
        Episode episode = findOwnedEpisode(episodeId, memberId);
        StoryPart part = episode.getStoryPart();
        Long novelId = part.getNovel().getId();
        bookmarkRepository.deleteByEpisodeId(episodeId);
        part.removeEpisode(episode);
        renumberEpisodes(part);
        refreshPartCompletion(part);
        return novelId;
    }

    @Transactional
    public void bulkChangeEpisodeStatus(Long novelId, Long memberId, List<Long> episodeIds, EpisodeStatus status) {
        if (episodeIds == null || episodeIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "회차를 선택해 주세요.");
        }
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "공개 상태를 확인해 주세요.");
        }
        Novel novel = findOwnedNovelWithContents(novelId, memberId);
        Set<Long> requested = new LinkedHashSet<>(episodeIds);
        List<Episode> targets = collectOwnedEpisodes(novel, requested);
        if (targets.size() != requested.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "선택한 회차 중 처리할 수 없는 항목이 있습니다.");
        }
        Set<StoryPart> touchedParts = new LinkedHashSet<>();
        for (Episode episode : targets) {
            episode.changeStatus(status);
            touchedParts.add(episode.getStoryPart());
        }
        for (StoryPart part : touchedParts) {
            refreshPartCompletion(part);
        }
    }

    @Transactional
    public Long bulkDeleteEpisodes(Long novelId, Long memberId, List<Long> episodeIds) {
        if (episodeIds == null || episodeIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "회차를 선택해 주세요.");
        }
        Novel novel = findOwnedNovelWithContents(novelId, memberId);
        Set<Long> requested = new LinkedHashSet<>(episodeIds);
        List<Episode> targets = collectOwnedEpisodes(novel, requested);
        if (targets.size() != requested.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "선택한 회차 중 삭제할 수 없는 항목이 있습니다.");
        }

        Map<StoryPart, List<Episode>> byPart = new LinkedHashMap<>();
        for (Episode episode : targets) {
            byPart.computeIfAbsent(episode.getStoryPart(), key -> new ArrayList<>()).add(episode);
        }
        for (Map.Entry<StoryPart, List<Episode>> entry : byPart.entrySet()) {
            StoryPart part = entry.getKey();
            for (Episode episode : entry.getValue()) {
                bookmarkRepository.deleteByEpisodeId(episode.getId());
                part.removeEpisode(episode);
            }
            renumberEpisodes(part);
            refreshPartCompletion(part);
        }
        return novelId;
    }

    private List<Episode> collectOwnedEpisodes(Novel novel, Set<Long> episodeIds) {
        List<Episode> found = new ArrayList<>();
        for (StoryPart part : novel.getParts()) {
            for (Episode episode : part.getEpisodes()) {
                if (episodeIds.contains(episode.getId())) {
                    found.add(episode);
                }
            }
        }
        return found;
    }

    private void refreshPartCompletion(StoryPart part) {
        if (!part.allEpisodesPublished() && part.getStatus() == StoryPartStatus.COMPLETED) {
            part.update(part.getTitle(), StoryPartStatus.SERIALIZING);
            if (part.getNovel().getStatus() == NovelStatus.COMPLETED) {
                part.getNovel().changeStatus(NovelStatus.SERIALIZING);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Episode> visibleEpisodes(StoryPart part, Long memberId, ExperienceRole role) {
        Novel novel = part.getNovel();
        boolean privileged = novel.isOwnedBy(memberId) || role == ExperienceRole.ADMIN;
        return readableEpisodes(part, privileged);
    }

    public List<StoryPart> latestParts(Novel novel) {
        return latestParts(novel, true);
    }

    public List<StoryPart> latestParts(Novel novel, boolean privileged) {
        return readableParts(novel, privileged).stream()
                .sorted(Comparator.comparing(StoryPart::getPartNumber).reversed())
                .toList();
    }

    private void renumberParts(Novel novel) {
        List<StoryPart> parts = sortedParts(novel);
        int offset = 10_000;
        for (int index = 0; index < parts.size(); index++) {
            parts.get(index).changePartNumber(offset + index + 1);
        }
        novelRepository.flush();
        for (int index = 0; index < parts.size(); index++) {
            parts.get(index).changePartNumber(index + 1);
        }
    }

    private void renumberEpisodes(StoryPart part) {
        List<Episode> episodes = part.getEpisodes().stream()
                .sorted(Comparator.comparing(Episode::getEpisodeNumber))
                .toList();
        int offset = 10_000;
        for (int index = 0; index < episodes.size(); index++) {
            episodes.get(index).changeEpisodeNumber(offset + index + 1);
        }
        storyPartRepository.flush();
        for (int index = 0; index < episodes.size(); index++) {
            episodes.get(index).changeEpisodeNumber(index + 1);
        }
    }

    private void initializeEpisodes(Novel novel) {
        novel.getGenres().size();
        novel.getParts().forEach(part -> part.getEpisodes().size());
    }

    private List<StoryPart> sortedParts(Novel novel) {
        return novel.getParts().stream()
                .sorted(Comparator.comparing(StoryPart::getPartNumber))
                .toList();
    }

    private List<StoryPart> readableParts(Novel novel, boolean privileged) {
        List<StoryPart> parts = sortedParts(novel);
        if (privileged) {
            return parts;
        }
        return parts.stream()
                .filter(part -> part.getStatus() != StoryPartStatus.UNPUBLISHED)
                .toList();
    }

    private List<Episode> readableEpisodes(StoryPart part, boolean privileged) {
        List<Episode> episodes = new ArrayList<>(part.getEpisodes());
        episodes.sort(Comparator.comparing(Episode::getEpisodeNumber));
        if (privileged) {
            return episodes;
        }
        return episodes.stream()
                .filter(episode -> episode.getStatus() == EpisodeStatus.PUBLISHED)
                .toList();
    }

    private int indexOf(List<Episode> episodes, Long episodeId) {
        for (int index = 0; index < episodes.size(); index++) {
            if (episodes.get(index).getId().equals(episodeId)) {
                return index;
            }
        }
        return -1;
    }

    private int indexOfPart(List<StoryPart> parts, Long partId) {
        for (int index = 0; index < parts.size(); index++) {
            if (parts.get(index).getId().equals(partId)) {
                return index;
            }
        }
        return -1;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
