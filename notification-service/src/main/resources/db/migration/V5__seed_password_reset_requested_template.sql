-- V5__seed_password_reset_requested_template.sql

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
VALUES (
    UUID(),
    'PASSWORD_RESET_REQUESTED_EMAIL_V1',
    'EMAIL',
    'Reset your ClearIt password',
    '<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Reset your ClearIt password</title>
</head>

<body style="
    margin:0;
    padding:0;
    background:#f4f7fb;
    font-family:Arial,Helvetica,sans-serif;
    color:#1f2937;
">

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
                   style="
                       max-width:600px;
                       background:#ffffff;
                       border-radius:16px;
                       overflow:hidden;
                       box-shadow:0 4px 20px rgba(15,23,42,0.08);
                   ">

                <!-- Header -->
                <tr>
                    <td style="
                        background:linear-gradient(135deg,#2563eb,#4f46e5);
                        padding:32px 40px;
                        text-align:center;
                    ">

                        <div style="
                            font-size:30px;
                            font-weight:800;
                            color:#ffffff;
                            letter-spacing:-0.5px;
                        ">
                            ClearIt
                        </div>

                        <div style="
                            margin-top:8px;
                            font-size:14px;
                            color:#dbeafe;
                        ">
                            Your preparation starts here.
                        </div>

                    </td>
                </tr>

                <!-- Icon + Title -->
                <tr>
                    <td style="
                        padding:42px 40px 10px;
                        text-align:center;
                    ">

                        <div style="
                            width:64px;
                            height:64px;
                            line-height:64px;
                            margin:0 auto 20px;
                            border-radius:50%;
                            background:#eff6ff;
                            color:#2563eb;
                            font-size:30px;
                            font-weight:bold;
                        ">
                            🔐
                        </div>

                        <h1 style="
                            margin:0;
                            font-size:26px;
                            line-height:1.3;
                            color:#111827;
                        ">
                            Reset your password
                        </h1>

                        <p style="
                            margin:18px 0 0;
                            font-size:16px;
                            line-height:1.7;
                            color:#6b7280;
                        ">
                            Hi {{firstName}},<br>
                            We received a request to reset the password
                            for your ClearIt account.
                        </p>

                    </td>
                </tr>

                <!-- CTA -->
                <tr>
                    <td style="
                        padding:30px 40px 20px;
                        text-align:center;
                    ">

                        <a href="{{resetUrl}}"
                           style="
                               display:inline-block;
                               background:#2563eb;
                               color:#ffffff;
                               text-decoration:none;
                               font-size:16px;
                               font-weight:700;
                               padding:15px 30px;
                               border-radius:10px;
                               box-shadow:0 4px 10px rgba(37,99,235,0.25);
                           ">
                            Reset My Password →
                        </a>

                    </td>
                </tr>

                <!-- Expiry notice -->
                <tr>
                    <td style="
                        padding:20px 40px 30px;
                    ">

                        <div style="
                            background:#eff6ff;
                            border-left:4px solid #2563eb;
                            border-radius:8px;
                            padding:16px 18px;
                        ">

                            <p style="
                                margin:0;
                                font-size:14px;
                                line-height:1.6;
                                color:#374151;
                            ">

                                <strong style="color:#1d4ed8;">
                                    This link expires in 15 minutes.
                                </strong>

                                <br>

                                For your security, the password reset link
                                can only be used once.

                            </p>

                        </div>

                    </td>
                </tr>

                <!-- Fallback URL -->
                <tr>
                    <td style="
                        padding:0 40px 34px;
                    ">

                        <p style="
                            margin:0;
                            font-size:13px;
                            line-height:1.7;
                            color:#9ca3af;
                        ">
                            If the button above does not work, copy and paste
                            the following link into your browser:
                        </p>

                        <p style="
                            margin:10px 0 0;
                            word-break:break-all;
                            font-size:13px;
                            line-height:1.6;
                        ">

                            <a href="{{resetUrl}}"
                               style="color:#2563eb;text-decoration:none;">
                                {{resetUrl}}
                            </a>

                        </p>

                    </td>
                </tr>

                <!-- Security notice -->
                <tr>
                    <td style="
                        padding:0 40px 30px;
                    ">

                        <div style="
                            background:#fff7ed;
                            border-left:4px solid #f97316;
                            border-radius:8px;
                            padding:16px 18px;
                        ">

                            <p style="
                                margin:0;
                                font-size:13px;
                                line-height:1.6;
                                color:#7c2d12;
                            ">

                                <strong>Didn''t request a password reset?</strong>

                                <br>

                                You can safely ignore this email.
                                Your password will remain unchanged.

                            </p>

                        </div>

                    </td>
                </tr>

                <!-- Footer -->
                <tr>
                    <td style="
                        border-top:1px solid #e5e7eb;
                        padding:24px 40px;
                        text-align:center;
                        background:#fafafa;
                    ">

                        <p style="
                            margin:0;
                            font-size:13px;
                            color:#9ca3af;
                        ">
                            © 2026 ClearIt. All rights reserved.
                        </p>

                        <p style="
                            margin:8px 0 0;
                            font-size:12px;
                            color:#d1d5db;
                        ">
                            This is an automated email. Please do not reply.
                        </p>

                    </td>
                </tr>

            </table>

        </td>
    </tr>

</table>

</body>
</html>',
    1,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
);