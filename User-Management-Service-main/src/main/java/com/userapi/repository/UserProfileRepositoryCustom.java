package com.userapi.repository;


import com.userapi.models.entity.UserProfile;

import java.util.List;

public interface UserProfileRepositoryCustom {
    List<UserProfile> findUsersWithFilters(
            String orgUuid,
            List<String> emails,
            List<String> usernames,
            List<String> statuses,
            List<String> firstNames,
            List<String> lastNames,
            List<String> phones
    );
}
