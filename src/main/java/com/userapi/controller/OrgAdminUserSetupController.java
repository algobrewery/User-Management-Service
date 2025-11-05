package com.userapi.controller;

import com.userapi.converters.CreateUserRequestConverter;
import com.userapi.converters.CreateUserResponseConverter;
import com.userapi.exception.DuplicateResourceException;
import com.userapi.models.external.CreateUserRequest;
import com.userapi.models.external.CreateUserResponse;
import com.userapi.models.internal.CreateUserInternalRequest;
import com.userapi.service.UserService;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.concurrent.ExecutionException;

import static com.userapi.common.constants.HeaderConstants.APP_CLIENT_USER_SESSION_UUID;
import static com.userapi.common.constants.HeaderConstants.APP_ORG_UUID;
import static com.userapi.common.constants.HeaderConstants.APP_REGION_ID;
import static com.userapi.common.constants.HeaderConstants.APP_TRACE_ID;

@RestController
@RequestMapping("/bootstrap-organization-admin")
public class OrgAdminUserSetupController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final CreateUserRequestConverter createUserRequestConverter;
    private final CreateUserResponseConverter createUserResponseConverter;
    private final UserService userService;

    @Autowired
    public OrgAdminUserSetupController(
            @Qualifier("CreateUserRequestConverter") CreateUserRequestConverter createUserRequestConverter,
            @Qualifier("CreateUserResponseConverter") CreateUserResponseConverter createUserResponseConverter,
            UserService userService) {
        this.createUserRequestConverter = createUserRequestConverter;
        this.createUserResponseConverter = createUserResponseConverter;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<CreateUserResponse> bootstrapOrganizationAdmin(
            @RequestHeader(APP_ORG_UUID) String orgUUID,
            @RequestHeader(APP_CLIENT_USER_SESSION_UUID) String clientUserSessionUUID,
            @RequestHeader(APP_TRACE_ID) String traceID,
            @RequestHeader(APP_REGION_ID) String regionID,
            @Valid @RequestBody CreateUserRequest request) {

        logger.info("Creating user for org: {}, traceId: {}", orgUUID, traceID);

        CreateUserInternalRequest internalRequest = createUserRequestConverter.toInternal(
                orgUUID,
                Strings.EMPTY,
                clientUserSessionUUID,
                traceID,
                regionID,
                request);
        try {
            return userService.createUser(internalRequest)
                    .thenApply(createUserResponseConverter::toExternal)
                    .thenApply(r -> new ResponseEntity<>(r, r.getHttpStatus()))
                    .get();
        } catch (ExecutionException e) {
            logger.error("Exception in creating user", e);
            Throwable cause = e.getCause();
            if (cause instanceof DuplicateResourceException) {
                return new ResponseEntity<>(
                        CreateUserResponse.builder()
                                .message(cause.getMessage())
                                .httpStatus(HttpStatus.BAD_REQUEST)
                                .build(),
                        HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(
                    CreateUserResponse.builder()
                            .message(e.getMessage())
                            .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                            .build(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (InterruptedException e) {
            logger.error("Exception in creating user", e);
            Thread.currentThread().interrupt();
            return new ResponseEntity<>(
                    CreateUserResponse.builder()
                            .message("Request interrupted")
                            .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                            .build(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
