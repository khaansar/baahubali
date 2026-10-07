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
) VALUES (
    UUID(),
    'USER_EMAIL_VERIFICATION_REQUESTED_EMAIL_V1',
    'EMAIL',
    'Verify your ClearIt email address',
    '<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>

<body style="margin:0;padding:0;background:#f4f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">

<table role="presentation"
       width="100%"
       cellspacing="0"
       cellpadding="0"
       border="0"
       style="background:#f4f7fb;padding:40px 16px;">

<tr>
<td align="center">

<table role="presentation"
       width="100%"
       cellspacing="0"
       cellpadding="0"
       border="0"
       style="max-width:600px;background:#ffffff;border-radius:16px;overflow:hidden;">

<tr>
<td style="background:linear-gradient(135deg,#2563eb,#4f46e5);padding:32px 40px;text-align:center;">

<div style="font-size:30px;font-weight:800;color:#ffffff;">
ClearIt
</div>

<div style="margin-top:8px;font-size:14px;color:#dbeafe;">
Your preparation starts here.
</div>

</td>
</tr>

<tr>
<td style="padding:42px 40px 20px;">

<div style="text-align:center;font-size:42px;margin-bottom:18px;">
✉️
</div>

<h1 style="margin:0;text-align:center;font-size:26px;color:#111827;">
Welcome to ClearIt, {{firstName}}!
</h1>

<p style="margin:20px 0 0;text-align:center;font-size:16px;line-height:1.7;color:#6b7280;">
Thanks for creating your ClearIt account.
Verify your email address to activate your account.
</p>

</td>
</tr>

<tr>
<td style="padding:20px 40px 34px;text-align:center;">

<a href="{{verificationUrl}}"
   style="display:inline-block;background:#2563eb;color:#ffffff;text-decoration:none;font-size:16px;font-weight:700;padding:15px 30px;border-radius:10px;">

Verify My Email →

</a>

</td>
</tr>

<tr>
<td style="padding:0 40px 30px;">

<div style="background:#eff6ff;border-left:4px solid #2563eb;border-radius:8px;padding:16px 18px;">

<p style="margin:0;font-size:14px;line-height:1.6;color:#374151;">

<strong style="color:#1d4ed8;">
Link expires in 15 minutes
</strong>

<br>

For your security, this verification link can only be used once.

</p>

</div>

</td>
</tr>

<tr>
<td style="padding:0 40px 34px;">

<p style="margin:0;font-size:13px;line-height:1.7;color:#9ca3af;">

If the button does not work, copy and paste this link into your browser:

</p>

<p style="margin:8px 0 0;word-break:break-all;font-size:13px;">

<a href="{{verificationUrl}}" style="color:#2563eb;">
{{verificationUrl}}
</a>

</p>

</td>
</tr>

<tr>
<td style="border-top:1px solid #e5e7eb;padding:24px 40px;text-align:center;background:#fafafa;">

<p style="margin:0;font-size:13px;line-height:1.6;color:#9ca3af;">

Didn''t create a ClearIt account?<br>
You can safely ignore this email.

</p>

</td>
</tr>

</table>

</td>
</tr>

</table>

</body>
</html>',
    2,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
);