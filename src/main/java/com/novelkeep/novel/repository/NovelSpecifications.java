package com.novelkeep.novel.repository;

import com.novelkeep.home.domain.ExperienceRole;
import com.novelkeep.novel.domain.Novel;
import com.novelkeep.novel.domain.NovelFavorite;
import com.novelkeep.novel.domain.NovelStatus;
import com.novelkeep.novel.domain.NovelVisibility;
import com.novelkeep.novel.domain.StoryPart;
import com.novelkeep.novel.domain.StoryPartStatus;
import com.novelkeep.novel.dto.NovelSearchCriteria;

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

        if (criteria.getGenre() != null) {
            predicate = cb.and(predicate, cb.equal(root.get("genre"), criteria.getGenre()));
        }

        if (criteria.isSerializingProgress()) {
            predicate = cb.and(predicate, cb.equal(root.get("status"), NovelStatus.SERIALIZING));
        } else if (criteria.isPartCompletedProgress()) {
            predicate = cb.and(
                    predicate,
                    cb.equal(root.get("status"), NovelStatus.SERIALIZING),
                    hasCompletedPart(root, query, cb)
            );
        } else if (criteria.isCompletedProgress()) {
            predicate = cb.and(predicate, cb.equal(root.get("status"), NovelStatus.COMPLETED));
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
