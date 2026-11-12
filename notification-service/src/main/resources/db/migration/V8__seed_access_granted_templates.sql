-- Sent when an admin manually unlocks a paid test series for a user.
-- {{reasonBlock}} and {{accessPeriod}} are pre-rendered (and escaped) by NotificationOrchestrator.
INSERT INTO notification_templates (id, template_code, channel, subject, body, version, active, created_at, updated_at)
VALUES
(
    UUID(),
    'ACCESS_GRANTED_IN_APP_V1',
    'IN_APP',
    'New test series unlocked',
    'You now have access to {{seriesTitle}}. Start practising whenever you are ready.',
    1,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
),
(
    UUID(),
    'ACCESS_GRANTED_EMAIL_V1',
    'EMAIL',
    'You now have access to {{seriesTitle}} | ClearIt',
    '<html><body style="margin:0;padding:0;background:#f1f5f9;font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif;">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f1f5f9;padding:32px 12px;"><tr><td align="center">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:560px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(15,23,42,0.06);">
<tr><td style="background:linear-gradient(135deg,#4f46e5,#7c3aed);background-color:#4f46e5;padding:28px 32px;">
<p style="margin:0;font-size:13px;font-weight:600;letter-spacing:0.08em;text-transform:uppercase;color:#e0e7ff;">Access unlocked</p>
<h1 style="margin:8px 0 0;font-size:24px;line-height:1.3;color:#ffffff;">{{seriesTitle}}</h1>
</td></tr>
<tr><td style="padding:28px 32px 8px;">
<p style="margin:0 0 14px;font-size:16px;color:#0f172a;">Hi {{firstName}},</p>
<p style="margin:0 0 14px;font-size:15px;line-height:1.6;color:#334155;">Good news! The ClearIt team has given you full access to <strong>{{seriesTitle}}</strong>. Every mock test in the series is now available in your account &mdash; no payment needed.</p>
<p style="margin:0;font-size:15px;line-height:1.6;color:#334155;">{{accessPeriod}}</p>
{{reasonBlock}}
</td></tr>
<tr><td style="padding:24px 32px 32px;">
<a href="{{seriesUrl}}" style="display:inline-block;background:#4f46e5;color:#ffffff;text-decoration:none;font-size:15px;font-weight:600;padding:12px 24px;border-radius:10px;">Start practising</a>
<p style="margin:24px 0 0;font-size:14px;line-height:1.6;color:#64748b;">All the best with your preparation!<br>The ClearIt Team</p>
</td></tr>
</table>
<p style="margin:16px 0 0;font-size:12px;color:#94a3b8;">You received this email because access was added to your ClearIt account.</p>
</td></tr></table>
</body></html>',
    1,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
);
