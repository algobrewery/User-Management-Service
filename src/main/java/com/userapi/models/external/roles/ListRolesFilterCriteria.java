package com.userapi.models.external.roles;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListRolesFilterCriteria {
    private List<ListRolesFilterCriteriaAttribute> attributes;
}
