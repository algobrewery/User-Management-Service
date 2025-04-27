package com.userapi.repository;

import com.userapi.models.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    @Query("SELECT u FROM UserProfile u WHERE u.username = :username OR u.email = :email OR u.phone = :phone")
    List<UserProfile> findConflictingUsers(@Param("username") String username,
                                           @Param("email") String email,
                                           @Param("phone") String phone);
}