package com.userapi.repository;

import com.userapi.models.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface UserProfileRepositoryCustom {
    Page<UserProfile> findUsersWithFilters(
            String orgUuid,
            Map<String, List<String>> filters,
            Pageable pageable
    );
}
