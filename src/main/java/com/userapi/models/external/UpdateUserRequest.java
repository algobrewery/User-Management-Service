package com.userapi.models.external;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.Valid;
import java.util.List;

@Data
public class UpdateUserRequest {
    private String username;
    private String firstName;
    private String middleName;
    private String lastName;
    private String status;
    private PhoneInfo phoneInfo;
    private EmailInfo emailInfo;
    
    // Support both single employmentInfo and employmentInfoList for backward compatibility
    @Valid
    @JsonProperty("employmentInfo")
    @JsonAlias("employmentInfoList")
    private List<EmploymentInfo> employmentInfoList;
    
    // Legacy field for backward compatibility - will be populated from employmentInfoList
    @Deprecated
    @JsonIgnore
    public EmploymentInfo getEmploymentInfo() {
        if (employmentInfoList != null && !employmentInfoList.isEmpty()) {
            return employmentInfoList.get(0);
        }
        return null;
    }
    
    // Legacy setter for backward compatibility
    @Deprecated
    @JsonIgnore
    public void setEmploymentInfo(EmploymentInfo employmentInfo) {
        if (employmentInfo != null) {
            this.employmentInfoList = List.of(employmentInfo);
        } else {
            this.employmentInfoList = null;
        }
    }
}
