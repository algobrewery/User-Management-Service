package com.userapi.repository;

import com.userapi.models.entity.UserReportee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserReporteeRepository extends JpaRepository<UserReportee, String> {
    List<UserReportee> findByManagerUserUuid(String managerUserUuid);
    List<UserReportee> findByJobProfileUuid(String jobProfileUuid);
}
