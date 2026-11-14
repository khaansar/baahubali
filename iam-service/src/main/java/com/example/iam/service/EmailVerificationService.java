package com.example.iam.service;

import com.example.iam.entity.User;
import com.example.iam.event.IamEventPublisher;
import com.example.iam.exception.InvalidOtpException;
import com.example.iam.exception.OtpCooldownException;
import com.example.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Verifies a user's email address with a one-time code sent to that address. */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final IamEventPublisher iamEventPublisher;

    /**
     * Emails a new verification code to the user.
     *
     * @throws OtpCooldownException if a code was sent too recently
     */
    public void sendVerificationCode(User user) {
        String code = otpService.issue(OtpService.Purpose.EMAIL_VERIFICATION, user.getId());

        iamEventPublisher.publishOtpRequested(
                user,
                IamEventPublisher.EMAIL_VERIFICATION_OTP_REQUESTED,
                code,
                otpService.ttl().toMinutes()
        );
    }

    @Transactional
    public void verifyEmail(String email, String code) {
        User user = userRepository.findByEmail(normalize(email))
                .filter(User::canSignIn)
                .orElseThrow(InvalidOtpException::new);

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return;
        }

        if (!otpService.verify(OtpService.Purpose.EMAIL_VERIFICATION, user.getId(), code)) {
            throw new InvalidOtpException();
        }

        user.setEmailVerified(true);
        userRepository.save(user);
    }

    /**
     * Sends a new code if the account exists and is unverified. Always completes silently so the
     * public endpoint does not reveal which emails are registered or whether a code was just sent.
     */
    public void resendVerificationCode(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        userRepository.findByEmail(normalize(email))
                .filter(User::canSignIn)
                .filter(user -> !Boolean.TRUE.equals(user.getEmailVerified()))
                .ifPresent(user -> {
                    try {
                        sendVerificationCode(user);
                    } catch (OtpCooldownException ignored) {
                        // A code is already on its way; the client shows its own resend timer.
                    }
                });
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
