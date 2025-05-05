package com.userapi.repository;

import com.userapi.models.entity.UserReportee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface UserReporteeRepository extends JpaRepository<UserReportee, String> {
    List<UserReportee> findByManagerUserUuid(String managerUserUuid);
    List<UserReportee> findByJobProfileUuid(String jobProfileUuid);
    @Transactional
    @Modifying
    @Query("DELETE FROM UserReportee ur WHERE ur.jobProfileUuid = :jobProfileUuid")
    void deleteByJobProfileUuid(@Param("jobProfileUuid") String jobProfileUuid);

}
