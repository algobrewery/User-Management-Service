package com.userapi.controller;

import com.userapi.dto.CreateUserRequest;
import com.userapi.dto.CreateUserResponse;
import com.userapi.dto.GetUserResponse;
import com.userapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

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
            @RequestHeader("x-app-org-uuid") String orgUuid,
            @RequestHeader("x-app-user-uuid") String userUuid,
            @RequestHeader("x-app-client-user-session-uuid") String sessionUuid,
            @RequestHeader("x-app-trace-id") String traceId,
            @RequestHeader("x-app-region-id") String regionId,
            @Valid @RequestBody CreateUserRequest request) {

        CreateUserResponse response = userService.createUser(request, orgUuid);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{jobProfileUuid}")
    public ResponseEntity<GetUserResponse> getUserByJobProfileId(
            @PathVariable String jobProfileUuid) {

        GetUserResponse response = userService.getUserByJobProfileId(jobProfileUuid);
        return ResponseEntity.ok(response);
}
}