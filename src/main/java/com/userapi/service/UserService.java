package com.userapi.service;

import com.userapi.models.external.*;

public interface UserService {

    CreateUserResponse createUser(CreateUserRequest request, String orgUuid);

    GetUserResponse getUser(String orgUUID, String userId);

    ListUsersResponse listUsers(ListUsersRequest request, String orgUuid);
}
