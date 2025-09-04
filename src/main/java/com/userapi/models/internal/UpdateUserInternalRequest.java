package com.userapi.models.internal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@Getter
@Setter
public class UpdateUserInternalRequest extends BaseInternalRequest {

    private String username;
    private String firstName;
    private String middleName;
    private String lastName;
    private String status;
    private PhoneInfoDto phoneInfo;
    private EmailInfoDto emailInfo;
    private List<EmploymentInfoDto> employmentInfoList;
    
    // Legacy field for backward compatibility
    @Deprecated
    @JsonIgnore
    public EmploymentInfoDto getEmploymentInfo() {
        if (employmentInfoList != null && !employmentInfoList.isEmpty()) {
            return employmentInfoList.get(0);
        }
        return null;
    }
    
    // Legacy setter for backward compatibility
    @Deprecated
    @JsonIgnore
    public void setEmploymentInfo(EmploymentInfoDto employmentInfo) {
        if (employmentInfo != null) {
            this.employmentInfoList = List.of(employmentInfo);
        } else {
            this.employmentInfoList = null;
        }
    }

}
