package com.userapi.models.external.roles;

import lombok.Data;

import java.util.List;

@Data
public class ListRolesSelector {
    private List<String> base_attributes;
    private List<String> extensions;
}
