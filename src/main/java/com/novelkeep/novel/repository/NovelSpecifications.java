package com.novelkeep.novel.repository;

import com.novelkeep.funding.domain.FundingCampaign;
import com.novelkeep.funding.domain.FundingCampaignStatus;
import com.novelkeep.home.domain.ExperienceRole;
import com.novelkeep.novel.domain.Novel;
import com.novelkeep.novel.domain.NovelFavorite;
import com.novelkeep.novel.domain.NovelStatus;
import com.novelkeep.novel.domain.NovelVisibility;
import com.novelkeep.novel.domain.StoryPart;
import com.novelkeep.novel.domain.StoryPartStatus;
import com.novelkeep.novel.dto.NovelSearchCriteria;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.springframework.data.jpa.domain.Specification;

public final class NovelSpecifications {

    private NovelSpecifications() {
    }

    public static Specification<Novel> publicBrowse(
            NovelSearchCriteria criteria,
            ExperienceRole role,
            Long memberId
    ) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (role == ExperienceRole.ADMIN) {
                if (criteria.getVisibility() != null) {
                    predicate = cb.and(predicate, cb.equal(root.get("visibility"), criteria.getVisibility()));
                }
            } else {
                predicate = cb.and(predicate, cb.equal(root.get("visibility"), NovelVisibility.PUBLIC));
            }

            if (criteria.isFavoriteOnly() && memberId != null) {
                predicate = cb.and(predicate, isFavoritedBy(root, query, cb, memberId));
            }

            predicate = cb.and(predicate, commonFilters(root, query, cb, criteria, false));
            return predicate;
        };
    }

    public static Specification<Novel> ownedBrowse(NovelSearchCriteria criteria, Long memberId) {
        return (root, query, cb) -> {
            Predicate predicate = cb.equal(root.get("author").get("id"), memberId);
            if (criteria.getVisibility() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("visibility"), criteria.getVisibility()));
            }
            predicate = cb.and(predicate, commonFilters(root, query, cb, criteria, true));
            return predicate;
        };
    }

    public static Specification<Novel> sorted(NovelSearchCriteria criteria) {
        return (root, query, cb) -> {
            Class<?> resultType = query.getResultType();
            if (resultType == Long.class || resultType == long.class) {
                return cb.conjunction();
            }

            Expression<String> updatedMinute = minuteBucket(cb, root.get("updatedAt"));
            Order updatedTieBreaker = cb.desc(updatedMinute);

            if (!criteria.hasSortSelection()) {
                query.orderBy(updatedTieBreaker);
                return cb.conjunction();
            }

            boolean ascending = criteria.isSortAscending();
            if (criteria.isRecommendSort()) {
                Order byRecommend = ascending
                        ? cb.asc(root.get("recommendationCount"))
                        : cb.desc(root.get("recommendationCount"));
                query.orderBy(byRecommend, updatedTieBreaker);
            } else if (criteria.isTitleSort()) {
                Order byTitle = ascending ? cb.asc(root.get("title")) : cb.desc(root.get("title"));
                query.orderBy(byTitle, updatedTieBreaker);
            } else {
                Order byUpdated = ascending ? cb.asc(updatedMinute) : cb.desc(updatedMinute);
                query.orderBy(byUpdated);
            }
            return cb.conjunction();
        };
    }

    private static Expression<String> minuteBucket(CriteriaBuilder cb, Expression<?> dateTime) {
        return cb.function("DATE_FORMAT", String.class, dateTime, cb.literal("%Y-%m-%d %H:%i"));
    }

    private static Predicate commonFilters(
            Root<Novel> root,
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            NovelSearchCriteria criteria,
            boolean titleOnlyKeyword
    ) {
        Predicate predicate = cb.conjunction();

        String keyword = criteria.getKeyword() == null ? "" : criteria.getKeyword().trim();
        if (!keyword.isEmpty()) {
            String pattern = "%" + keyword.toLowerCase() + "%";
            if (titleOnlyKeyword) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("title")), pattern));
            } else {
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("penName")), pattern),
                        cb.like(cb.lower(root.get("synopsis")), pattern)
                ));
            }
        }

        if (criteria.hasGenres()) {
            for (var genre : criteria.getGenres()) {
                predicate = cb.and(predicate, cb.isMember(genre, root.get("genres")));
            }
        }

        if (criteria.isSerializingProgress()) {
            predicate = cb.and(predicate, cb.equal(root.get("status"), NovelStatus.SERIALIZING));
        } else if (criteria.isPartCompletedProgress()) {
            predicate = cb.and(
                    predicate,
                    cb.equal(root.get("status"), NovelStatus.SERIALIZING),
                    hasCompletedPart(root, query, cb)
            );
        } else         if (criteria.isCompletedProgress()) {
            predicate = cb.and(predicate, cb.equal(root.get("status"), NovelStatus.COMPLETED));
        }

        if (criteria.hasFundingOpenFilter()) {
            Predicate hasOpen = hasOpenFunding(root, query, cb);
            predicate = cb.and(predicate, criteria.isFundingOpenOnly() ? hasOpen : cb.not(hasOpen));
        }

        return predicate;
    }

    private static Predicate hasCompletedPart(
            Root<Novel> root,
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder cb
    ) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<StoryPart> partRoot = subquery.from(StoryPart.class);
        subquery.select(partRoot.get("id"));
        subquery.where(
                cb.equal(partRoot.get("novel").get("id"), root.get("id")),
                cb.equal(partRoot.get("status"), StoryPartStatus.COMPLETED)
        );
        return cb.exists(subquery);
    }

    private static Predicate hasOpenFunding(
            Root<Novel> root,
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder cb
    ) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<FundingCampaign> campaignRoot = subquery.from(FundingCampaign.class);
        subquery.select(campaignRoot.get("id"));
        subquery.where(
                cb.equal(campaignRoot.get("storyPart").get("novel").get("id"), root.get("id")),
                cb.equal(campaignRoot.get("status"), FundingCampaignStatus.OPEN)
        );
        return cb.exists(subquery);
    }

    private static Predicate isFavoritedBy(
            Root<Novel> root,
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            Long memberId
    ) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<NovelFavorite> favoriteRoot = subquery.from(NovelFavorite.class);
        subquery.select(favoriteRoot.get("id"));
        subquery.where(
                cb.equal(favoriteRoot.get("novel").get("id"), root.get("id")),
                cb.equal(favoriteRoot.get("member").get("id"), memberId)
        );
        return cb.exists(subquery);
    }
}
