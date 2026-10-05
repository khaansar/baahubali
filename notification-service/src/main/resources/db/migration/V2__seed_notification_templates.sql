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
) VALUES
(
    UUID(),
    'OTP_REQUESTED_EMAIL_V1',
    'EMAIL',
    'Your Baahubali verification code',
    '<html><body><h2>Baahubali verification</h2><p>Your verification code is <strong>{{otp}}</strong>.</p><p>This code expires soon. If you did not request it, you can ignore this email.</p></body></html>',
    1,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
),
(
    UUID(),
    'USER_REGISTERED_EMAIL_V1',
    'EMAIL',
    'Welcome to Baahubali',
    '<html><body><h2>Welcome to Baahubali, {{firstName}}!</h2><p>Your account has been created successfully.</p></body></html>',
    1,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
),
(
    UUID(),
    'PASSWORD_RESET_EMAIL_V1',
    'EMAIL',
    'Reset your Baahubali password',
    '<html><body><h2>Password reset</h2><p>Your password reset code is <strong>{{otp}}</strong>.</p></body></html>',
    1,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
),
(
    UUID(),
    'PAYMENT_SUCCESS_EMAIL_V1',
    'EMAIL',
    'Payment successful - Baahubali',
    '<html><body><h2>Payment successful</h2><p>Your payment of <strong>{{amount}}</strong> was successful.</p><p>Payment ID: {{paymentId}}</p></body></html>',
    1,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
),
(
    UUID(),
    'PAYMENT_FAILED_EMAIL_V1',
    'EMAIL',
    'Payment failed - Baahubali',
    '<html><body><h2>Payment failed</h2><p>We could not complete your payment.</p><p>Payment ID: {{paymentId}}</p></body></html>',
    1,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
),
(
    UUID(),
    'PAYMENT_REFUNDED_EMAIL_V1',
    'EMAIL',
    'Payment refunded - Baahubali',
    '<html><body><h2>Payment refunded</h2><p>Your refund of <strong>{{amount}}</strong> has been initiated.</p><p>Payment ID: {{paymentId}}</p></body></html>',
    1,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
),
(
    UUID(),
    'ATTEMPT_SUBMITTED_EMAIL_V1',
    'EMAIL',
    'Test submitted - Baahubali',
    '<html><body><h2>Test submitted</h2><p>Your attempt <strong>{{attemptId}}</strong> has been submitted successfully.</p><p>Test ID: {{testId}}</p></body></html>',
    1,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
);
