package com.example.iam.service;

import com.example.common.audit.AuditEvent;
import com.example.iam.entity.Role;
import com.example.iam.entity.User;
import com.example.iam.event.IamEventPublisher;
import com.example.iam.exception.AccountActionNotAllowedException;
import com.example.iam.exception.InvalidOtpException;
import com.example.iam.exception.OtpCooldownException;
import com.example.iam.exception.UserNotFoundException;
import com.example.iam.repository.UserIdentityRepository;
import com.example.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Self-service account deletion, confirmed with a code emailed to the account's address.
 *
 * The user row is kept (other services reference the user id for attempts, reports and payments)
 * but is anonymised: personal details are cleared, the email is replaced so it can be registered
 * again, linked sign-in providers are removed, and every session is ended.
 */
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final OtpService otpService;
    private final IamEventPublisher iamEventPublisher;
    private final AuthService authService;
    private final AuditLogService auditLogService;

    /**
     * Emails a deletion confirmation code to the user.
     *
     * @return minutes until the code expires
     * @throws OtpCooldownException if a code was sent too recently
     */
    public long sendDeletionCode(UUID userId) {
        User user = loadDeletableUser(userId);
        String code = otpService.issue(OtpService.Purpose.ACCOUNT_DELETION, userId);
        long expiresInMinutes = otpService.ttl().toMinutes();

        iamEventPublisher.publishOtpRequested(
                user,
                IamEventPublisher.ACCOUNT_DELETION_OTP_REQUESTED,
                code,
                expiresInMinutes
        );

        return expiresInMinutes;
    }

    @Transactional
    public void deleteAccount(UUID userId, String code) {
        User user = loadDeletableUser(userId);

        if (!otpService.verify(OtpService.Purpose.ACCOUNT_DELETION, userId, code)) {
            throw new InvalidOtpException();
        }

        user.setEmail("deleted-" + userId + "@deleted.invalid");
        user.setFirstName("Deleted");
        user.setLastName("user");
        user.setPhone(null);
        user.setTargetExam(null);
        user.setAvatarUrl(null);
        user.setPasswordHash(null);
        user.setEmailVerified(false);
        user.setIsActive(false);
        user.setDeletedAt(Instant.now());
        userRepository.save(user);

        userIdentityRepository.deleteByUserId(userId);

        auditLogService.store(new AuditEvent(
                UUID.randomUUID(),
                userId.toString(),
                user.getRole().name(),
                "ACCOUNT_DELETED",
                "USER",
                userId.toString(),
                "iam-service",
                "/users/me/delete",
                "POST",
                200,
                null,
                Map.of(),
                Instant.now()
        ));

        runAfterCommit(() -> authService.logout(userId.toString()));
    }

    private User loadDeletableUser(UUID userId) {
        User user = userRepository.findById(userId)
                .filter(User::canSignIn)
                .orElseThrow(UserNotFoundException::new);

        if (user.getRole() == Role.ADMIN) {
            throw new AccountActionNotAllowedException(
                    "Admin accounts cannot be deleted from the profile page. "
                            + "Ask another admin to remove your admin access first."
            );
        }

        return user;
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
