package com.userapi.repository;

import com.userapi.models.entity.UserProfile;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

@Repository
public class UserProfileRepositoryImpl implements UserProfileRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<UserProfile> findUsersWithFilters(
            String orgUuid,
            List<String> emails,
            List<String> usernames,
            List<String> statuses,
            List<String> firstNames,
            List<String> lastNames,
            List<String> phones
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserProfile> query = cb.createQuery(UserProfile.class);
        Root<UserProfile> root = query.from(UserProfile.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("organizationUuid"), orgUuid));

        if (emails != null && !emails.isEmpty()) {
            predicates.add(root.get("email").in(emails));
        }
        if (usernames != null && !usernames.isEmpty()) {
            predicates.add(root.get("username").in(usernames));
        }
        if (statuses != null && !statuses.isEmpty()) {
            predicates.add(root.get("status").in(statuses));
        }
        if (firstNames != null && !firstNames.isEmpty()) {
            predicates.add(root.get("firstName").in(firstNames));
        }
        if (lastNames != null && !lastNames.isEmpty()) {
            predicates.add(root.get("lastName").in(lastNames));
        }
        if (phones != null && !phones.isEmpty()) {
            predicates.add(root.get("phone").in(phones));
        }

        query.select(root).where(cb.and(predicates.toArray(new Predicate[0])));
        return entityManager.createQuery(query).getResultList();
    }
}
