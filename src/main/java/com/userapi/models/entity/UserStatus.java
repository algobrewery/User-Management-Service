package com.userapi.models.entity;

import lombok.Getter;

@Getter
public enum UserStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive");

    private final String name;

    UserStatus(String name) {
        this.name = name;
    }
}
