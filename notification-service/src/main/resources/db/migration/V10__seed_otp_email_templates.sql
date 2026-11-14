-- Email verification and account deletion now use 6-digit one-time codes instead of links.
-- Placeholders: {{firstName}}, {{otp}}, {{expiresInMinutes}}.

-- The link-based verification email is no longer sent.
UPDATE notification_templates
SET active = FALSE, updated_at = CURRENT_TIMESTAMP(6)
WHERE template_code = 'USER_EMAIL_VERIFICATION_REQUESTED_EMAIL_V1';

INSERT INTO notification_templates (id, template_code, channel, subject, body, version, active, created_at, updated_at)
VALUES
(
    UUID(),
    'USER_EMAIL_VERIFICATION_OTP_REQUESTED_EMAIL_V1',
    'EMAIL',
    '{{otp}} is your ClearIt verification code',
    '<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
<body style="margin:0;padding:0;background:#f4f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background:#f4f7fb;padding:40px 16px;">
<tr><td align="center">
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="max-width:600px;background:#ffffff;border-radius:16px;overflow:hidden;">
<tr><td style="background:linear-gradient(135deg,#2563eb,#4f46e5);padding:32px 40px;text-align:center;">
<div style="font-size:30px;font-weight:800;color:#ffffff;">ClearIt</div>
<div style="margin-top:8px;font-size:14px;color:#dbeafe;">Your preparation starts here.</div>
</td></tr>
<tr><td style="padding:42px 40px 12px;">
<h1 style="margin:0;text-align:center;font-size:26px;color:#111827;">Verify your email, {{firstName}}</h1>
<p style="margin:20px 0 0;text-align:center;font-size:16px;line-height:1.7;color:#6b7280;">Enter this code on the verification screen to activate your ClearIt account.</p>
</td></tr>
<tr><td style="padding:20px 40px 30px;text-align:center;">
<div style="display:inline-block;background:#eff6ff;border:1px solid #bfdbfe;border-radius:12px;padding:16px 28px;font-size:34px;font-weight:800;letter-spacing:10px;color:#1d4ed8;font-family:''Courier New'',Courier,monospace;">{{otp}}</div>
</td></tr>
<tr><td style="padding:0 40px 30px;">
<div style="background:#eff6ff;border-left:4px solid #2563eb;border-radius:8px;padding:16px 18px;">
<p style="margin:0;font-size:14px;line-height:1.6;color:#374151;"><strong style="color:#1d4ed8;">This code expires in {{expiresInMinutes}} minutes.</strong><br>Never share it with anyone. ClearIt will never ask you for this code.</p>
</div>
</td></tr>
<tr><td style="border-top:1px solid #e5e7eb;padding:24px 40px;text-align:center;background:#fafafa;">
<p style="margin:0;font-size:13px;line-height:1.6;color:#9ca3af;">Didn''t create a ClearIt account?<br>You can safely ignore this email.</p>
</td></tr>
</table>
</td></tr>
</table>
</body>
</html>',
    1,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
),
(
    UUID(),
    'ACCOUNT_DELETION_OTP_REQUESTED_EMAIL_V1',
    'EMAIL',
    '{{otp}} is your code to delete your ClearIt account',
    '<!DOCTYPE html>
<html lang="en">
<head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
<body style="margin:0;padding:0;background:#f4f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background:#f4f7fb;padding:40px 16px;">
<tr><td align="center">
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="max-width:600px;background:#ffffff;border-radius:16px;overflow:hidden;">
<tr><td style="background:#334155;padding:32px 40px;text-align:center;">
<div style="font-size:30px;font-weight:800;color:#ffffff;">ClearIt</div>
<div style="margin-top:8px;font-size:14px;color:#cbd5e1;">Account deletion request</div>
</td></tr>
<tr><td style="padding:42px 40px 12px;">
<h1 style="margin:0;text-align:center;font-size:26px;color:#111827;">Confirm account deletion</h1>
<p style="margin:20px 0 0;text-align:center;font-size:16px;line-height:1.7;color:#6b7280;">Hi {{firstName}}, we received a request to permanently delete your ClearIt account. Enter this code to confirm.</p>
</td></tr>
<tr><td style="padding:20px 40px 30px;text-align:center;">
<div style="display:inline-block;background:#fef2f2;border:1px solid #fecaca;border-radius:12px;padding:16px 28px;font-size:34px;font-weight:800;letter-spacing:10px;color:#b91c1c;font-family:''Courier New'',Courier,monospace;">{{otp}}</div>
</td></tr>
<tr><td style="padding:0 40px 30px;">
<div style="background:#fef2f2;border-left:4px solid #dc2626;border-radius:8px;padding:16px 18px;">
<p style="margin:0;font-size:14px;line-height:1.6;color:#374151;"><strong style="color:#b91c1c;">This code expires in {{expiresInMinutes}} minutes.</strong><br>Deleting your account removes your profile and signs you out everywhere. This cannot be undone.</p>
</div>
</td></tr>
<tr><td style="border-top:1px solid #e5e7eb;padding:24px 40px;text-align:center;background:#fafafa;">
<p style="margin:0;font-size:13px;line-height:1.6;color:#9ca3af;">Didn''t ask to delete your account?<br>Ignore this email and consider changing your password.</p>
</td></tr>
</table>
</td></tr>
</table>
</body>
</html>',
    1,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
);
