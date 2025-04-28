package com.userapi.controller;

import com.userapi.models.external.CreateUserRequest;
import com.userapi.models.external.CreateUserResponse;
import com.userapi.models.external.GetUserResponse;
import com.userapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

import static com.userapi.common.constants.HeaderConstants.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<CreateUserResponse> createUser(
            @RequestHeader(APP_ORG_UUID) String orgUUID,
            @RequestHeader(APP_USER_UUID) String userUUID,
            @RequestHeader(APP_CLIENT_USER_SESSION_UUID) String clientUserSessionUUID,
            @RequestHeader(APP_TRACE_ID) String traceID,
            @RequestHeader(APP_REGION_ID) String regionID,
            @Valid @RequestBody CreateUserRequest request) {

        CreateUserResponse response = userService.createUser(request, orgUUID);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<GetUserResponse> getUserById(
            @PathVariable String userId) {

        GetUserResponse response = userService.getUser(userId);
        return ResponseEntity.ok(response);
    }
}
