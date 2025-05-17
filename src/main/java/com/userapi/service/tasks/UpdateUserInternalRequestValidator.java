package com.userapi.service.tasks;

import com.userapi.exception.DuplicateResourceException;
import com.userapi.models.entity.UserProfile;
import com.userapi.models.internal.UpdateUserInternalRequest;
import com.userapi.repository.userprofile.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;

@Component("UpdateUserInternalRequestValidator")
@RequiredArgsConstructor
public class UpdateUserInternalRequestValidator {

    private static final Logger logger = LoggerFactory.getLogger(UpdateUserInternalRequestValidator.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    // Repositories
    private final UserProfileRepository userProfileRepository;

    public CompletableFuture<UpdateUserInternalRequest> validateUniqueUser(
            UserProfile userProfile,
            UpdateUserInternalRequest request) {
        logger.info("validateUniqueUser request:{}", request);
        String orgUUID = request.getRequestContext().getAppOrgUuid();
        List<CompletableFuture<UserProfile>> futures = new ArrayList<>();
        if (!isNull(request.getUsername())) {
            futures.add(
                    CompletableFuture.supplyAsync(() ->
                                            userProfileRepository.findUserByUsername(orgUUID, request.getUsername()),
                                    executor)
                            .completeOnTimeout(null, 500, TimeUnit.MILLISECONDS));
        }
        if (!isNull(request.getPhoneInfo())) {
            futures.add(
                    CompletableFuture.supplyAsync(() ->
                                            userProfileRepository.findUserByPhoneNumber(orgUUID, request.getPhoneInfo().getNumber()),
                                    executor)
                            .completeOnTimeout(null, 500, TimeUnit.MILLISECONDS));
        }
        if (!isNull(request.getEmailInfo())) {
            futures.add(
                    CompletableFuture.supplyAsync(() ->
                                            userProfileRepository.findUserByEmail(orgUUID, request.getEmailInfo().getEmail()),
                                    executor)
                            .completeOnTimeout(null, 500, TimeUnit.MILLISECONDS));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList())
                .thenCompose(matchingUsers -> {
                    boolean isUnique = Optional.ofNullable(matchingUsers)
                            .orElseGet(Collections::emptyList)
                            .stream()
                            .filter(Objects::nonNull)
                            .filter(v -> !v.getUserUuid().equals(userProfile.getUserUuid()))
                            .toList()
                            .isEmpty();
                    if (isUnique) {
                        return CompletableFuture.completedFuture(request);
                    }
                    return CompletableFuture.failedFuture(new DuplicateResourceException("found user matching username, or email, or phone"));
                });
    }

}
