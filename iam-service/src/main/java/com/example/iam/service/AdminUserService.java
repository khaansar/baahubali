package com.example.iam.service;

import com.example.common.audit.AuditEvent;
import com.example.iam.entity.User;
import com.example.iam.exception.AccountActionNotAllowedException;
import com.example.iam.exception.UserNotFoundException;
import com.example.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final AuditLogService auditLogService;

    /** Deactivates or reactivates a user. Deactivation ends the user's sessions immediately. */
    @Transactional
    public User setActive(UUID adminId, UUID userId, boolean active) {
        if (adminId.equals(userId)) {
            throw new AccountActionNotAllowedException("You cannot change the status of your own account.");
        }

        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        if (user.getDeletedAt() != null) {
            throw new AccountActionNotAllowedException("Deleted accounts cannot be reactivated or deactivated.");
        }

        if (Boolean.valueOf(active).equals(user.getIsActive())) {
            return user;
        }

        user.setIsActive(active);
        User saved = userRepository.save(user);

        auditLogService.store(new AuditEvent(
                UUID.randomUUID(),
                adminId.toString(),
                "ADMIN",
                active ? "USER_REACTIVATED" : "USER_DEACTIVATED",
                "USER",
                userId.toString(),
                "iam-service",
                "/admin/users/" + userId + "/status",
                "PATCH",
                200,
                null,
                Map.of(),
                Instant.now()
        ));

        if (!active) {
            runAfterCommit(() -> authService.logout(userId.toString()));
        }

        return saved;
    }

    private static void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
