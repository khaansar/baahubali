-- Sent when an admin removes a user's access to a test series.
-- {{reasonBlock}} is pre-rendered (and escaped) by NotificationOrchestrator; empty when no reason was given.
INSERT INTO notification_templates (id, template_code, channel, subject, body, version, active, created_at, updated_at)
VALUES
(
    UUID(),
    'ACCESS_REVOKED_IN_APP_V1',
    'IN_APP',
    'Test series access removed',
    'Your access to {{seriesTitle}} has been removed. Your past attempts and results are still in your history.',
    1,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
),
(
    UUID(),
    'ACCESS_REVOKED_EMAIL_V1',
    'EMAIL',
    'Update on your access to {{seriesTitle}} | ClearIt',
    '<html><body style="margin:0;padding:0;background:#f1f5f9;font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif;">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f1f5f9;padding:32px 12px;"><tr><td align="center">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:560px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(15,23,42,0.06);">
<tr><td style="background-color:#334155;padding:28px 32px;">
<p style="margin:0;font-size:13px;font-weight:600;letter-spacing:0.08em;text-transform:uppercase;color:#cbd5e1;">Access update</p>
<h1 style="margin:8px 0 0;font-size:24px;line-height:1.3;color:#ffffff;">{{seriesTitle}}</h1>
</td></tr>
<tr><td style="padding:28px 32px 8px;">
<p style="margin:0 0 14px;font-size:16px;color:#0f172a;">Hi {{firstName}},</p>
<p style="margin:0 0 14px;font-size:15px;line-height:1.6;color:#334155;">We are letting you know that your access to <strong>{{seriesTitle}}</strong> has been removed from your ClearIt account. Tests in this series can no longer be started.</p>
<p style="margin:0;font-size:15px;line-height:1.6;color:#334155;">Don&rsquo;t worry &mdash; every attempt you have already made, along with your scores and solutions, stays in your test history.</p>
{{reasonBlock}}
</td></tr>
<tr><td style="padding:24px 32px 32px;">
<a href="{{historyUrl}}" style="display:inline-block;background:#4f46e5;color:#ffffff;text-decoration:none;font-size:15px;font-weight:600;padding:12px 24px;border-radius:10px;">View my test history</a>
<p style="margin:20px 0 0;font-size:14px;line-height:1.6;color:#64748b;">Think this is a mistake? <a href="{{supportUrl}}" style="color:#4f46e5;font-weight:600;text-decoration:none;">Contact our support team</a> and we will look into it. You can also <a href="{{seriesUrl}}" style="color:#4f46e5;font-weight:600;text-decoration:none;">view the series</a> at any time.</p>
<p style="margin:20px 0 0;font-size:14px;line-height:1.6;color:#64748b;">Regards,<br>The ClearIt Team</p>
</td></tr>
</table>
<p style="margin:16px 0 0;font-size:12px;color:#94a3b8;">You received this email because access on your ClearIt account changed.</p>
</td></tr></table>
</body></html>',
    1,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
);
