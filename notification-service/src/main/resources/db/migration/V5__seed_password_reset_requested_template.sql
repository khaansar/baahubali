INSERT INTO notification_templates 
(name, event_type, channel, subject_template, body_template, version, is_active)
VALUES (
    'PASSWORD_RESET_REQUESTED_EMAIL_V1',
    'PASSWORD_RESET_REQUESTED',
    'EMAIL',
    'Password Reset Request - ClearIt',
    '<!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <title>Password Reset Request</title>
        <style>
            body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px; }
            .container { background-color: #ffffff; padding: 20px; border-radius: 5px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); max-width: 600px; margin: auto; }
            h2 { color: #333333; }
            p { font-size: 16px; color: #555555; }
            .btn { display: inline-block; padding: 10px 20px; background-color: #007bff; color: #ffffff; text-decoration: none; border-radius: 5px; font-weight: bold; margin-top: 20px; }
            .footer { margin-top: 30px; font-size: 12px; color: #999999; text-align: center; }
        </style>
    </head>
    <body>
        <div class="container">
            <h2>Password Reset Request</h2>
            <p>Hi {{firstName}},</p>
            <p>We received a request to reset your password for your ClearIt account. If you did not make this request, please ignore this email.</p>
            <p>To reset your password, click the button below:</p>
            <a href="{{resetUrl}}" class="btn">Reset Password</a>
            <p>Or copy and paste this link into your browser:</p>
            <p><a href="{{resetUrl}}">{{resetUrl}}</a></p>
            <div class="footer">
                <p>&copy; 2026 ClearIt. All rights reserved.</p>
            </div>
        </div>
    </body>
    </html>',
    1,
    true
);
