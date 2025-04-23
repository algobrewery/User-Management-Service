package com.userapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.userapi.dto.CreateUserRequest;
import com.userapi.dto.CreateUserResponse;
import com.userapi.entity.JobProfile;
import com.userapi.entity.UserProfile;
import com.userapi.exception.DuplicateResourceException;
import com.userapi.repository.JobProfileRepository;
import com.userapi.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserProfileRepository userProfileRepository;
    private final JobProfileRepository jobProfileRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public UserService(UserProfileRepository userProfileRepository, JobProfileRepository jobProfileRepository, ObjectMapper objectMapper) {
        this.userProfileRepository = userProfileRepository;
        this.jobProfileRepository = jobProfileRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request, String orgUuid) {
        // Check for duplicates
        List<UserProfile> conflictingUsers = userProfileRepository.findConflictingUsers(
                request.getUsername(),
                request.getEmailInfo().getEmail(),
                request.getPhoneInfo().getNumber()
        );

        for (UserProfile user : conflictingUsers) {
            if (user.getUsername().equals(request.getUsername())) {
                throw new DuplicateResourceException("Username already exists");
            }
            if (user.getEmail().equals(request.getEmailInfo().getEmail())) {
                throw new DuplicateResourceException("Email already exists");
            }
            if (user.getPhone().equals(request.getPhoneInfo().getNumber())) {
                throw new DuplicateResourceException("Phone number already exists");
            }
        }


        // Create JobProfile
        JobProfile jobProfile = new JobProfile();
        String jobProfileUuid = UUID.randomUUID().toString();
        jobProfile.setJobProfileUuid(jobProfileUuid);
        jobProfile.setOrganizationUuid(orgUuid);
        jobProfile.setTitle(request.getEmploymentInfo().getJobTitle());
        jobProfile.setStartDate(request.getEmploymentInfo().getStartDate());
        jobProfile.setReportingManager(request.getEmploymentInfo().getReportingManager());
        jobProfile.setOrganizationUnit(request.getEmploymentInfo().getOrganizationUnit());

        try {
            jobProfile.setExtensionsData(objectMapper.writeValueAsString(request.getEmploymentInfo().getExtensionsData()));
        } catch (Exception e) {
            // Handle exception appropriately
            jobProfile.setExtensionsData("{}");
        }

        jobProfileRepository.save(jobProfile);

        // Create UserProfile
        UserProfile userProfile = new UserProfile();
        String userUuid = UUID.randomUUID().toString();
        userProfile.setUserUuid(userUuid);
        userProfile.setOrganizationUuid(orgUuid);
        userProfile.setUsername(request.getUsername());
        userProfile.setFirstName(request.getFirstName());
        userProfile.setMiddleName(request.getMiddleName());
        userProfile.setLastName(request.getLastName());
        userProfile.setStartDate(request.getEmploymentInfo().getStartDate());
        userProfile.setEmail(request.getEmailInfo().getEmail());
        userProfile.setEmailVerificationStatus(request.getEmailInfo().getVerificationStatus());
        userProfile.setPhone(request.getPhoneInfo().getNumber());
        userProfile.setPhoneCountryCode(request.getPhoneInfo().getCountryCode());
        userProfile.setPhoneVerificationStatus(request.getPhoneInfo().getVerificationStatus());
        userProfile.setStatus("Active");

        // Set job profile uuid
        userProfile.setJobProfileUuids(new String[]{jobProfileUuid});

        userProfileRepository.save(userProfile);

        // Create response
        CreateUserResponse response = new CreateUserResponse();
        response.setUserId(userUuid);
        response.setUsername(request.getUsername());
        response.setStatus("Active");
        response.setMessage("User created successfully");

        return response;
    }
}