package com.userapi.repository;

import com.userapi.models.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {

    @Query("SELECT u FROM UserProfile u WHERE u.organizationUuid = :orgUuid AND (u.username = :username OR u.email = :email OR u.phone = :phone)")
    List<UserProfile> findUsersMatchingAny(@Param("orgUuid") String orgUuid,
                                           @Param("username") String username,
                                           @Param("email") String email,
                                           @Param("phone") String phone);

    @Query("SELECT u FROM UserProfile u WHERE u.organizationUuid = :orgUuid AND u.user_uuid = :userUuid")
    UserProfile findByUserId(@Param("orgUuid") String orgUuid,
                             @Param("userUuid") String userUuid);

    @Query("SELECT u FROM UserProfile u WHERE u.organizationUuid = :orgUuid " +
            "AND (:email IS NULL OR u.email IN :email) " +
            "AND (:username IS NULL OR u.username IN :username) " +
            "AND (:status IS NULL OR u.status IN :status) " +
            "AND (:firstName IS NULL OR u.firstName IN :firstName) " +
            "AND (:lastName IS NULL OR u.lastName IN :lastName) " +
            "AND (:phone IS NULL OR u.phone IN :phone)")
    List<UserProfile> findUsersWithFilters(
            @Param("orgUuid") String orgUuid,
            @Param("email") List<String> email,
            @Param("username") List<String> username,
            @Param("status") List<String> status,
            @Param("firstName") List<String> firstName,
            @Param("lastName") List<String> lastName,
            @Param("phone") List<String> phone);
}


