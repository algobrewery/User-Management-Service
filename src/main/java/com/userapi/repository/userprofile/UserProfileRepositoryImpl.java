package com.userapi.repository.userprofile;

import com.userapi.models.entity.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class UserProfileRepositoryImpl implements UserProfileRepositoryCustom {
    private static final Logger logger = LoggerFactory.getLogger(UserProfileRepositoryImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    private String convertToSnakeCase(String input) {
        return input.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    @Override
    public Page<UserProfile> findUsersWithFilters(
            String orgUuid,
            Map<String, List<String>> filters,
            Pageable pageable) {

        logger.info("Starting user search for org: {}, page: {}, size: {}",
                orgUuid, pageable.getPageNumber(), pageable.getPageSize());
        logger.debug("Filter criteria: {}", filters);

        try {
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<UserProfile> query = cb.createQuery(UserProfile.class);
            Root<UserProfile> root = query.from(UserProfile.class);

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organizationUuid"), orgUuid));

            // Handle base user profile filters
            handleBaseFilters(cb, root, predicates, filters);

            // Create a subquery for job profile filters
            if (filters.containsKey("jobTitle") || filters.containsKey("organizationUnit") ||
                    filters.containsKey("reportingManager")) {

                logger.debug("Using native query for job profile filters");

                // Use native PostgreSQL query for array operations
                String nativeQuery = buildNativeQuery(filters, pageable);
                logger.debug("Generated native query: {}", nativeQuery);

                javax.persistence.Query nativeQueryObj = entityManager.createNativeQuery(nativeQuery, UserProfile.class);
                setNativeQueryParameters(nativeQueryObj, orgUuid, filters, pageable);

                List<UserProfile> results = nativeQueryObj.getResultList();
                logger.debug("Found {} results from native query", results.size());

                // Get total count
                String countQuery = buildCountQuery(filters);
                logger.debug("Generated count query: {}", countQuery);

                javax.persistence.Query countQueryObj = entityManager.createNativeQuery(countQuery);
                setNativeQueryParameters(countQueryObj, orgUuid, filters, null);

                Long total = ((Number) countQueryObj.getSingleResult()).longValue();
                logger.info("Total matching records: {}", total);

                return new PageImpl<>(results, pageable, total);
            }

            // If no job profile filters, use the regular criteria query
            logger.debug("Using criteria query for base filters");
            query.select(root).where(cb.and(predicates.toArray(new Predicate[0])));

            // Add sorting
            if (pageable.getSort().isSorted()) {
                List<Order> orders = new ArrayList<>();
                pageable.getSort().forEach(order -> {
                    if (order.isAscending()) {
                        orders.add(cb.asc(root.get(order.getProperty())));
                    } else {
                        orders.add(cb.desc(root.get(order.getProperty())));
                    }
                });
                query.orderBy(orders);
                logger.debug("Applied sorting: {}", pageable.getSort());
            }

            TypedQuery<UserProfile> typedQuery = entityManager.createQuery(query);
            typedQuery.setFirstResult((int) pageable.getOffset());
            typedQuery.setMaxResults(pageable.getPageSize());

            // Get total count for non-job profile filters
            CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
            Root<UserProfile> countRoot = countQuery.from(UserProfile.class);
            countQuery.select(cb.count(countRoot)).where(cb.and(predicates.toArray(new Predicate[0])));
            Long total = entityManager.createQuery(countQuery).getSingleResult();
            logger.info("Total matching records: {}", total);

            List<UserProfile> results = typedQuery.getResultList();
            logger.debug("Found {} results from criteria query", results.size());

            return new PageImpl<>(results, pageable, total);
        } catch (Exception e) {
            logger.error("Error while searching users with filters: ", e);
            throw e;
        }
    }

    private String buildNativeQuery(Map<String, List<String>> filters, Pageable pageable) {
        StringBuilder query = new StringBuilder(
                "SELECT DISTINCT up.* FROM user_profiles up " +
                        "JOIN job_profiles jp ON jp.job_profile_uuid = ANY(up.job_profile_uuids) " +
                        "WHERE up.organization_uuid = :orgUuid"
        );

        if (filters.containsKey("jobTitle")) {
            query.append(" AND jp.title IN :jobTitles");
        }
        if (filters.containsKey("organizationUnit")) {
            query.append(" AND jp.organization_unit IN :organizationUnits");
        }
        if (filters.containsKey("reportingManager")) {
            query.append(" AND jp.reporting_manager IN :reportingManagers");
        }

        if (pageable.getSort().isSorted()) {
            String sortProperty = convertToSnakeCase(pageable.getSort().iterator().next().getProperty());
            String sortDirection = pageable.getSort().iterator().next().getDirection().name();
            query.append(" ORDER BY up.").append(sortProperty).append(" ").append(sortDirection);
        }

        query.append(" LIMIT :pageSize OFFSET :offset");
        return query.toString();
    }

    private String buildCountQuery(Map<String, List<String>> filters) {
        StringBuilder query = new StringBuilder(
                "SELECT COUNT(DISTINCT up.user_uuid) FROM user_profiles up " +
                        "JOIN job_profiles jp ON jp.job_profile_uuid = ANY(up.job_profile_uuids) " +
                        "WHERE up.organization_uuid = :orgUuid"
        );

        if (filters.containsKey("jobTitle")) {
            query.append(" AND jp.title IN :jobTitles");
        }
        if (filters.containsKey("organizationUnit")) {
            query.append(" AND jp.organization_unit IN :organizationUnits");
        }
        if (filters.containsKey("reportingManager")) {
            query.append(" AND jp.reporting_manager IN :reportingManagers");
        }

        return query.toString();
    }

    private void setNativeQueryParameters(
            javax.persistence.Query query,
            String orgUuid,
            Map<String, List<String>> filters,
            Pageable pageable) {

        query.setParameter("orgUuid", orgUuid);

        if (filters.containsKey("jobTitle")) {
            query.setParameter("jobTitles", filters.get("jobTitle"));
        }
        if (filters.containsKey("organizationUnit")) {
            query.setParameter("organizationUnits", filters.get("organizationUnit"));
        }
        if (filters.containsKey("reportingManager")) {
            query.setParameter("reportingManagers", filters.get("reportingManager"));
        }

        if (pageable != null) {
            query.setParameter("pageSize", pageable.getPageSize());
            query.setParameter("offset", pageable.getOffset());
        }
    }

    private void handleBaseFilters(CriteriaBuilder cb, Root<UserProfile> root, List<Predicate> predicates, Map<String, List<String>> filters) {
        logger.debug("Applying base filters: {}", filters.keySet());

        if (filters.containsKey("email")) {
            predicates.add(root.get("email").in(filters.get("email")));
        }
        if (filters.containsKey("username")) {
            predicates.add(root.get("username").in(filters.get("username")));
        }
        if (filters.containsKey("status")) {
            predicates.add(root.get("status").in(filters.get("status")));
        }
        if (filters.containsKey("firstName")) {
            predicates.add(root.get("firstName").in(filters.get("firstName")));
        }
        if (filters.containsKey("lastName")) {
            predicates.add(root.get("lastName").in(filters.get("lastName")));
        }
        if (filters.containsKey("phone")) {
            predicates.add(root.get("phone").in(filters.get("phone")));
        }
    }
}
