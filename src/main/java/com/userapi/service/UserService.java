package com.userapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.userapi.dto.*;
        import com.userapi.entity.JobProfile;
import com.userapi.entity.UserProfile;
import com.userapi.exception.DuplicateResourceException;
import com.userapi.exception.ResourceNotFoundException;
import com.userapi.repository.JobProfileRepository;
import com.userapi.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
        import java.util.stream.Collectors;

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

        // Create JobProfiles
        List<String> jobProfileUuids = new ArrayList<>();

        for (EmploymentInfo employmentInfo : request.getEmploymentInfo()) {
            JobProfile jobProfile = new JobProfile();
            String jobProfileUuid = UUID.randomUUID().toString();
            jobProfile.setJobProfileUuid(jobProfileUuid);
            jobProfile.setOrganizationUuid(orgUuid);
            jobProfile.setTitle(employmentInfo.getJobTitle());
            jobProfile.setStartDate(employmentInfo.getStartDate());
            jobProfile.setEndDate(employmentInfo.getEndDate());
            jobProfile.setReportingManager(employmentInfo.getReportingManager());
            jobProfile.setOrganizationUnit(employmentInfo.getOrganizationUnit());

            try {
                jobProfile.setExtensionsData(objectMapper.writeValueAsString(employmentInfo.getExtensionsData()));
            } catch (Exception e) {
                // Handle exception appropriately
                jobProfile.setExtensionsData("{}");
            }

            jobProfileRepository.save(jobProfile);
            jobProfileUuids.add(jobProfileUuid);
        }

        // Create UserProfile
        UserProfile userProfile = new UserProfile();
        String userUuid = UUID.randomUUID().toString();
        userProfile.setUserUuid(userUuid);
        userProfile.setOrganizationUuid(orgUuid);
        userProfile.setUsername(request.getUsername());
        userProfile.setFirstName(request.getFirstName());
        userProfile.setMiddleName(request.getMiddleName());
        userProfile.setLastName(request.getLastName());

        // Use the start date of the earliest job profile
        LocalDateTime earliestStartDate = request.getEmploymentInfo().stream()
                .map(EmploymentInfo::getStartDate)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        userProfile.setStartDate(earliestStartDate);
        userProfile.setEmail(request.getEmailInfo().getEmail());
        userProfile.setEmailVerificationStatus(request.getEmailInfo().getVerificationStatus());
        userProfile.setPhone(request.getPhoneInfo().getNumber());
        userProfile.setPhoneCountryCode(request.getPhoneInfo().getCountryCode());
        userProfile.setPhoneVerificationStatus(request.getPhoneInfo().getVerificationStatus());
        userProfile.setStatus("Active");

        // Set job profile uuids
        userProfile.setJobProfileUuids(jobProfileUuids.toArray(new String[0]));

        userProfileRepository.save(userProfile);

        // Create response
        CreateUserResponse response = new CreateUserResponse();
        response.setUserId(userUuid);
        response.setUsername(request.getUsername());
        response.setStatus("Active");
        response.setMessage("User created successfully");

        return response;
    }

    @Transactional(readOnly = true)
    public GetUserResponse getUserByJobProfileId(String jobProfileUuid) {
        // 1) Find the JobProfile
        JobProfile jobProfile = jobProfileRepository.findById(jobProfileUuid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Job profile not found: " + jobProfileUuid));

        // 2) Find the user who has this jobProfileUuid
        UserProfile user = userProfileRepository.findAll().stream()
                .filter(u -> Arrays.asList(u.getJobProfileUuids()).contains(jobProfileUuid))
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException("No user found for this job profile: " + jobProfileUuid));

        // 3) Build the basic response
        GetUserResponse response = new GetUserResponse();
        response.setUserId(user.getUserUuid());
        response.setUsername(user.getUsername());
        response.setFirstName(user.getFirstName());
        response.setMiddleName(user.getMiddleName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setPhone(formatPhoneNumber(user));
        response.setStartDate(user.getStartDate());
        response.setEndDate(user.getEndDate());
        response.setStatus(user.getStatus());

        // 4) Fetch and sort all of this user's job profiles
        List<JobProfile> allJobProfiles = Arrays.stream(user.getJobProfileUuids())
                .map(jobProfileRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparing(JobProfile::getStartDate).reversed())
                .collect(Collectors.toList());

        // 5) Determine current vs previous
        JobProfile current = allJobProfiles.stream()
                .filter(jp -> jp.getEndDate() == null)
                .findFirst()
                .orElse(allJobProfiles.isEmpty() ? null : allJobProfiles.get(0));

        if (current != null) {
            response.setCurrentJobProfile(convertToJobProfileDTO(current));
        }

        List<JobProfileDTO> previous = allJobProfiles.stream()
                .filter(jp -> current == null
                        ? jp.getEndDate() != null
                        : !jp.getJobProfileUuid().equals(current.getJobProfileUuid()) && jp.getEndDate() != null)
                .map(this::convertToJobProfileDTO)
                .collect(Collectors.toList());

        response.setPreviousJobProfiles(previous);
        return response;
    }


    private String formatPhoneNumber(UserProfile user) {
        return "+" + user.getPhoneCountryCode() + user.getPhone();
    }

    private JobProfileDTO convertToJobProfileDTO(JobProfile jobProfile) {
        JobProfileDTO dto = new JobProfileDTO();
        dto.setJobTitle(jobProfile.getTitle());
        dto.setStartDate(jobProfile.getStartDate());
        dto.setEndDate(jobProfile.getEndDate());
        dto.setReportingManager(jobProfile.getReportingManager());
        dto.setOrganizationUnit(jobProfile.getOrganizationUnit());



        // Parse extensions data
        try {
            JsonNode extensionsData = objectMapper.readTree(jobProfile.getExtensionsData());
            dto.setExtensionsData(extensionsData);
        } catch (Exception e) {
            // If parsing fails, set empty object
            dto.setExtensionsData(objectMapper.createObjectNode());
        }

        return dto;
    }
}