package com.userapi.repository;

import com.userapi.models.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class UserProfileRepositoryImpl implements UserProfileRepositoryCustom {

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

            // Use native PostgreSQL query for array operations
            String nativeQuery = "SELECT DISTINCT up.* FROM user_profiles up " +
                    "JOIN job_profiles jp ON jp.job_profile_uuid = ANY(up.job_profile_uuids) " +
                    "WHERE up.organization_uuid = :orgUuid";

            if (filters.containsKey("jobTitle")) {
                nativeQuery += " AND jp.title IN :jobTitles";
            }
            if (filters.containsKey("organizationUnit")) {
                nativeQuery += " AND jp.organization_unit IN :organizationUnits";
            }
            if (filters.containsKey("reportingManager")) {
                nativeQuery += " AND jp.reporting_manager IN :reportingManagers";
            }

            // Add sorting with snake_case column name
            if (pageable.getSort().isSorted()) {
                String sortProperty = convertToSnakeCase(pageable.getSort().iterator().next().getProperty());
                String sortDirection = pageable.getSort().iterator().next().getDirection().name();
                nativeQuery += " ORDER BY up." + sortProperty + " " + sortDirection;
            }

            nativeQuery += " LIMIT :pageSize OFFSET :offset";

            javax.persistence.Query nativeQueryObj = entityManager.createNativeQuery(nativeQuery, UserProfile.class);
            nativeQueryObj.setParameter("orgUuid", orgUuid);

            if (filters.containsKey("jobTitle")) {
                nativeQueryObj.setParameter("jobTitles", filters.get("jobTitle"));
            }
            if (filters.containsKey("organizationUnit")) {
                nativeQueryObj.setParameter("organizationUnits", filters.get("organizationUnit"));
            }
            if (filters.containsKey("reportingManager")) {
                nativeQueryObj.setParameter("reportingManagers", filters.get("reportingManager"));
            }

            nativeQueryObj.setParameter("pageSize", pageable.getPageSize());
            nativeQueryObj.setParameter("offset", pageable.getOffset());

            List<UserProfile> results = nativeQueryObj.getResultList();

            // Get total count
            String countQuery = "SELECT COUNT(DISTINCT up.user_uuid) FROM user_profiles up " +
                    "JOIN job_profiles jp ON jp.job_profile_uuid = ANY(up.job_profile_uuids) " +
                    "WHERE up.organization_uuid = :orgUuid";

            if (filters.containsKey("jobTitle")) {
                countQuery += " AND jp.title IN :jobTitles";
            }
            if (filters.containsKey("organizationUnit")) {
                countQuery += " AND jp.organization_unit IN :organizationUnits";
            }
            if (filters.containsKey("reportingManager")) {
                countQuery += " AND jp.reporting_manager IN :reportingManagers";
            }

            javax.persistence.Query countQueryObj = entityManager.createNativeQuery(countQuery);
            countQueryObj.setParameter("orgUuid", orgUuid);

            if (filters.containsKey("jobTitle")) {
                countQueryObj.setParameter("jobTitles", filters.get("jobTitle"));
            }
            if (filters.containsKey("organizationUnit")) {
                countQueryObj.setParameter("organizationUnits", filters.get("organizationUnit"));
            }
            if (filters.containsKey("reportingManager")) {
                countQueryObj.setParameter("reportingManagers", filters.get("reportingManager"));
            }

            Long total = ((Number) countQueryObj.getSingleResult()).longValue();

            return new PageImpl<>(results, pageable, total);
        }

        // If no job profile filters, use the regular criteria query
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
        }

        TypedQuery<UserProfile> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        // Get total count for non-job profile filters
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<UserProfile> countRoot = countQuery.from(UserProfile.class);
        countQuery.select(cb.count(countRoot)).where(cb.and(predicates.toArray(new Predicate[0])));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(typedQuery.getResultList(), pageable, total);
    }

    private void handleBaseFilters(CriteriaBuilder cb, Root<UserProfile> root, List<Predicate> predicates, Map<String, List<String>> filters) {
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
