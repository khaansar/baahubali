
INSERT INTO notification_templates (
    id,
    template_code,
    channel,
    subject,
    body,
    version,
    active,
    created_at,
    updated_at
)
VALUES
(
    UUID(),
    'ATTEMPT_SUBMITTED_IN_APP_V1',
    'IN_APP',
    'Test submitted successfully',
    'Your test has been submitted successfully. View your result and review your performance.',
    1,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
),
(
    UUID(),
    'PAYMENT_CAPTURED_IN_APP_V1',
    'IN_APP',
    'Payment successful',
    'Your payment was successful! Your purchased test series is now ready to access.',
    1,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
),
(
    UUID(),
    'PAYMENT_CAPTURED_EMAIL_V1',
    'EMAIL',
    'Payment successful | ClearIt',
    '<html><body><h2>Payment successful!</h2><p>Hi {{firstName}},</p><p>Your payment of <strong>{{amount}} {{currency}}</strong> has been received successfully.</p><p>Your purchased test series: <strong>{{seriesTitle}}</strong></p><p><a href="{{seriesUrl}}">Access your test series</a></p><p><strong>Order ID:</strong> {{orderId}}</p><p>Thank you for choosing ClearIt. Happy learning!</p><p>Regards,<br>The ClearIt Team</p></body></html>',
    1,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
),
(
    UUID(),
    'STREAK_AT_RISK_EMAIL_V1',
    'EMAIL',
    'Keep your learning streak alive | ClearIt',
    '<html><body><h2>Your streak is waiting!</h2><p>Hi {{firstName}},</p><p>You have an opportunity to keep your learning streak going. Submit at least one test today to maintain your consistency.</p><p><a href="{{testsUrl}}">Explore tests on ClearIt</a></p><p>Every attempt brings you one step closer to your goal. Keep going!</p><p>Regards,<br>The ClearIt Team</p></body></html>',
    1,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
);
