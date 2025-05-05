package com.userapi.service;

import com.userapi.models.external.*;
import com.userapi.models.internal.CreateUserInternalRequest;
import com.userapi.models.internal.CreateUserInternalResponse;


public interface UserService {
    CreateUserInternalResponse createUser(CreateUserInternalRequest request);
    GetUserResponse getUser(String orgUUID, String userId);
    ListUsersResponse listUsers(ListUsersRequest request, String orgUuid);
    UpdateUserResponse updateUser(String orgUuid, String userId, UpdateUserRequest request);
    UpdateUserResponse deactivateUser(String orgUuid, String userId);
    UserHierarchyResponse getUserHierarchy(String orgUUID, String userId);

}
