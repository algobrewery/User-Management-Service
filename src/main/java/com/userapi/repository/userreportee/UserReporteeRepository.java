package com.userapi.repository.userreportee;

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

    @Query("SELECT r FROM UserReportee r WHERE r.organizationUuid = :organizationUuid AND r.managerUserUuid = :managerUserUuid")
    List<UserReportee> findByManagerUserUuid(
            @Param("organizationUuid") String organizationUuid,
            @Param("managerUserUuid") String managerUserUuid);

    @Query("SELECT r FROM UserReportee r WHERE r.organizationUuid = :organizationUuid AND r.jobProfileUuid = :jobProfileUuid")
    List<UserReportee> findByJobProfileUuid(
            @Param("organizationUuid") String organizationUuid,
            @Param("jobProfileUuid") String jobProfileUuid);

    @Transactional
    @Modifying
    @Query("DELETE FROM UserReportee ur WHERE ur.jobProfileUuid = :jobProfileUuid")
    void deleteByJobProfileUuid(@Param("jobProfileUuid") String jobProfileUuid);

}
