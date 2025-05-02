package com.userapi.models.external;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ListUsersFilterCriteriaAttribute {
    @NotBlank(message = "Attribute name is required")
    private String name;

    @NotNull(message = "Attribute values are required")
    private List<String> values;
}
