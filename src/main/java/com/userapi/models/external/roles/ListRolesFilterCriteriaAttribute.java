package com.userapi.models.external.roles;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListRolesFilterCriteriaAttribute {
    @NotBlank(message = "Attribute name is required")
    private String name;

    @NotNull(message = "Attribute values are required")
    private List<String> values;
}
