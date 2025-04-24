package com.userapi.dto;

import java.time.LocalDateTime;
import java.util.List;

public class GetUserResponse {
    private String userId;
    private String username;
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
    private JobProfileDTO currentJobProfile;
    private List<JobProfileDTO> previousJobProfiles;

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public JobProfileDTO getCurrentJobProfile() {
        return currentJobProfile;
    }

    public void setCurrentJobProfile(JobProfileDTO currentJobProfile) {
        this.currentJobProfile = currentJobProfile;
    }

    public List<JobProfileDTO> getPreviousJobProfiles() {
        return previousJobProfiles;
    }

    public void setPreviousJobProfiles(List<JobProfileDTO> previousJobProfiles) {
        this.previousJobProfiles = previousJobProfiles;
    }
}