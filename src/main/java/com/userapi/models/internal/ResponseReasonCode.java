package com.userapi.models.internal;

public enum ResponseReasonCode {
    SUCCESS,
    CREATED_SUCCESSFULLY, // ✅ Add this
    BAD_REQUEST,
    ENTITY_NOT_FOUND,
    DUPLICATED_ENTITY,
    INTERNAL_SERVER_ERROR,
    EXTERNAL_ERROR,
    UNKNOWN
}
