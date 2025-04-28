package com.userapi.controller;

import com.userapi.models.external.CreateUserRequest;
import com.userapi.models.external.CreateUserResponse;
import com.userapi.models.external.GetUserResponse;
import com.userapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static com.userapi.common.constants.HeaderConstants.*;

@RestController
@RequestMapping("/user") // Base path for user operations
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // POST http://52.23.186.179:8080/user
    @PostMapping
    public ResponseEntity<CreateUserResponse> createUser(
            @RequestHeader(APP_ORG_UUID) String orgUuid,
            @RequestHeader(APP_USER_UUID) String userUuid,
            @RequestHeader(APP_CLIENT_USER_SESSION_UUID) String sessionUuid,
            @RequestHeader(APP_TRACE_ID) String traceId,
            @RequestHeader(APP_REGION_ID) String regionId,
            @Valid @RequestBody CreateUserRequest request) {

        CreateUserResponse response = userService.createUser(request, orgUuid);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET http://52.23.186.179:8080/user/{userId}
    @GetMapping("/{userId}")
    public ResponseEntity<GetUserResponse> getUser(
            @PathVariable String userId) {

        GetUserResponse response = userService.getUser(userId);
        return ResponseEntity.ok(response);
    }
}