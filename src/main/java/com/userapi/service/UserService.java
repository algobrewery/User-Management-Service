package com.userapi.service;

import com.userapi.models.external.GetUserResponse;
import com.userapi.models.external.ListUsersRequest;
import com.userapi.models.external.ListUsersResponse;
import com.userapi.models.external.UpdateUserRequest;
import com.userapi.models.external.UpdateUserResponse;
import com.userapi.models.external.UserHierarchyResponse;
import com.userapi.models.internal.CreateUserInternalRequest;
import com.userapi.models.internal.CreateUserInternalResponse;
import com.userapi.models.internal.GetUserInternalRequest;
import com.userapi.models.internal.GetUserInternalResponse;

import java.util.concurrent.CompletableFuture;


public interface UserService {
    CompletableFuture<CreateUserInternalResponse> createUser(CreateUserInternalRequest request);
    CompletableFuture<GetUserInternalResponse> getUser(GetUserInternalRequest request);
    ListUsersResponse listUsers(ListUsersRequest request, String orgUuid);
    UpdateUserResponse updateUser(String orgUuid, String userId, UpdateUserRequest request);
    UpdateUserResponse deactivateUser(String orgUuid, String userId);
    UserHierarchyResponse getUserHierarchy(String orgUUID, String userId);

}
