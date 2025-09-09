package com.userapi.models.external.roles;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class ListRolesFilterCriteriaAttribute {
    @NotBlank(message = "Attribute name is required")
    private String name;

    @NotNull(message = "Attribute values are required")
    private List<String> values;
}
