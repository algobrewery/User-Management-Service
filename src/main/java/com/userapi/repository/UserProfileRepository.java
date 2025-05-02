package com.userapi.repository;

import com.userapi.models.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String>, UserProfileRepositoryCustom {
    @Query("SELECT u FROM UserProfile u WHERE u.organizationUuid = :orgUuid AND (u.username = :username OR u.email = :email OR u.phone = :phone)")
    List<UserProfile> findUsersMatchingAny(@Param("orgUuid") String orgUuid,
                                           @Param("username") String username,
                                           @Param("email") String email,
                                           @Param("phone") String phone);

    @Query("SELECT u FROM UserProfile u WHERE u.organizationUuid = :orgUuid AND u.userUuid = :userUuid")
    UserProfile findByUserId(@Param("orgUuid") String orgUuid,
                             @Param("userUuid") String userUuid);
}
