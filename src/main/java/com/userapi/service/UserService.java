package com.userapi.service;

import com.userapi.models.external.GetUserResponse;
import com.userapi.models.external.ListUsersRequest;
import com.userapi.models.external.ListUsersResponse;
import com.userapi.models.internal.CreateUserInternalRequest;
import com.userapi.models.internal.CreateUserInternalResponse;
import com.userapi.models.external.UpdateUserRequest;
import com.userapi.models.external.UpdateUserResponse;


public interface UserService {
    CreateUserInternalResponse createUser(CreateUserInternalRequest request);
    GetUserResponse getUser(String orgUUID, String userId);
    ListUsersResponse listUsers(ListUsersRequest request, String orgUuid);
    UpdateUserResponse updateUser(String orgUuid, String userId, UpdateUserRequest request);
    UpdateUserResponse deactivateUser(String orgUuid, String userId);

}
