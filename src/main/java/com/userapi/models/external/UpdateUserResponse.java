package com.userapi.models.external;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateUserResponse {
    private String userId;
    private String message;
    private String status;  // Add this field
}
