"""
Failure email notifications for report compilation errors.

Sends a premium HTML email when a report compilation fails, with details
about the report, execution, error, and the user who triggered it.
"""
import logging
import threading
from django.conf import settings
from django.core.mail import send_mail
from django.utils import timezone as tz

logger = logging.getLogger(__name__)


def send_failure_notification(report, execution, error_message: str, user):
    """
    Send an HTML failure notification email in a background thread.
    Non-blocking — will not slow down the API response.
    """
    recipients = _get_recipients()
    if not recipients:
        logger.warning("No FAILURE_NOTIFICATION_EMAIL configured — skipping notification.")
        return

    thread = threading.Thread(
        target=_send_email,
        args=(report, execution, error_message, user, recipients),
        daemon=True,
    )
    thread.start()


def _get_recipients() -> list[str]:
    raw = getattr(settings, 'FAILURE_NOTIFICATION_EMAIL', '')
    if not raw:
        return []
    return [e.strip() for e in raw.split(',') if e.strip()]


def _send_email(report, execution, error_message: str, user, recipients: list[str]):
    try:
        report_name = getattr(report, 'display_name', None) or report.report_name
        user_display = getattr(user, 'display_name', None) or user.username
        user_email = getattr(user, 'email', '') or ''
        timestamp = tz.now().strftime('%B %d, %Y at %H:%M:%S %Z')
        exec_id = execution.id if execution else 'N/A'
        report_id = report.id if report else 'N/A'
        output_format = getattr(execution, 'output_format', 'PDF') or 'PDF'

        frontend_url = getattr(settings, 'FRONTEND_BASE_URL', 'http://localhost:5173')
        report_link = f"{frontend_url}/reports/{report_id}"

        subject = f"⚠️ Report Failed: {report_name}"

        html = _build_html(
            report_name=report_name,
            report_id=str(report_id),
            exec_id=str(exec_id),
            error_message=error_message,
            user_display=user_display,
            user_email=user_email,
            timestamp=timestamp,
            output_format=output_format.upper(),
            report_link=report_link,
        )

        plain = (
            f"Report Compilation Failed\n\n"
            f"Report: {report_name} (ID: {report_id})\n"
            f"Execution: {exec_id}\n"
            f"Format: {output_format}\n"
            f"Triggered by: {user_display} ({user_email})\n"
            f"Time: {timestamp}\n\n"
            f"Error:\n{error_message}\n"
        )

        send_mail(
            subject=subject,
            message=plain,
            from_email=settings.DEFAULT_FROM_EMAIL,
            recipient_list=recipients,
            html_message=html,
            fail_silently=False,
        )
        logger.info(f"Failure notification sent to {recipients} for report '{report_name}'")

    except Exception as e:
        logger.error(f"Failed to send failure notification email: {e}")


def _build_html(
    report_name: str,
    report_id: str,
    exec_id: str,
    error_message: str,
    user_display: str,
    user_email: str,
    timestamp: str,
    output_format: str,
    report_link: str,
) -> str:
    """Build a premium HTML email for failure notification."""

    # Escape HTML entities in the error message
    import html
    safe_error = html.escape(error_message)

    return f"""\
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Report Failure Notification</title>
</head>
<body style="margin:0;padding:0;background-color:#0f172a;font-family:'Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif;">

<!-- Outer wrapper -->
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#0f172a;padding:32px 16px;">
<tr><td align="center">

<!-- Card -->
<table role="presentation" width="560" cellpadding="0" cellspacing="0" style="background-color:#1e293b;border-radius:16px;overflow:hidden;box-shadow:0 25px 50px rgba(0,0,0,0.5);">

  <!-- Red accent bar -->
  <tr>
    <td style="height:4px;background:linear-gradient(90deg,#ef4444,#f97316,#ef4444);"></td>
  </tr>

  <!-- Header -->
  <tr>
    <td style="padding:32px 32px 16px 32px;">
      <table role="presentation" width="100%" cellpadding="0" cellspacing="0">
        <tr>
          <td>
            <div style="display:inline-block;width:48px;height:48px;background:linear-gradient(135deg,#ef4444,#dc2626);border-radius:12px;text-align:center;line-height:48px;font-size:24px;">
              &#9888;
            </div>
          </td>
          <td style="padding-left:16px;">
            <h1 style="margin:0;font-size:20px;font-weight:700;color:#f1f5f9;letter-spacing:-0.02em;">
              Report Compilation Failed
            </h1>
            <p style="margin:4px 0 0 0;font-size:13px;color:#94a3b8;">
              {timestamp}
            </p>
          </td>
        </tr>
      </table>
    </td>
  </tr>

  <!-- Divider -->
  <tr>
    <td style="padding:0 32px;">
      <div style="height:1px;background-color:rgba(148,163,184,0.1);"></div>
    </td>
  </tr>

  <!-- Report details -->
  <tr>
    <td style="padding:24px 32px 16px 32px;">
      <h2 style="margin:0 0 16px 0;font-size:13px;font-weight:600;color:#94a3b8;text-transform:uppercase;letter-spacing:0.08em;">
        Report Details
      </h2>
      <table role="presentation" width="100%" cellpadding="0" cellspacing="0">
        <tr>
          <td style="padding:8px 12px;background-color:rgba(15,23,42,0.5);border-radius:8px 8px 0 0;border-bottom:1px solid rgba(148,163,184,0.06);">
            <span style="font-size:12px;color:#64748b;display:block;">Report Name</span>
            <span style="font-size:14px;color:#f1f5f9;font-weight:600;">{html.escape(report_name)}</span>
          </td>
        </tr>
        <tr>
          <td style="padding:8px 12px;background-color:rgba(15,23,42,0.5);border-bottom:1px solid rgba(148,163,184,0.06);">
            <table role="presentation" width="100%"><tr>
              <td width="50%">
                <span style="font-size:12px;color:#64748b;display:block;">Report ID</span>
                <span style="font-size:14px;color:#e2e8f0;font-family:monospace;">{report_id}</span>
              </td>
              <td width="50%">
                <span style="font-size:12px;color:#64748b;display:block;">Execution ID</span>
                <span style="font-size:14px;color:#e2e8f0;font-family:monospace;">{exec_id}</span>
              </td>
            </tr></table>
          </td>
        </tr>
        <tr>
          <td style="padding:8px 12px;background-color:rgba(15,23,42,0.5);border-bottom:1px solid rgba(148,163,184,0.06);">
            <table role="presentation" width="100%"><tr>
              <td width="50%">
                <span style="font-size:12px;color:#64748b;display:block;">Triggered By</span>
                <span style="font-size:14px;color:#e2e8f0;">{html.escape(user_display)}</span>
              </td>
              <td width="50%">
                <span style="font-size:12px;color:#64748b;display:block;">Output Format</span>
                <span style="display:inline-block;padding:2px 10px;border-radius:6px;background-color:rgba(99,102,241,0.15);color:#a5b4fc;font-size:12px;font-weight:600;">{output_format}</span>
              </td>
            </tr></table>
          </td>
        </tr>
        <tr>
          <td style="padding:8px 12px;background-color:rgba(15,23,42,0.5);border-radius:0 0 8px 8px;">
            <span style="font-size:12px;color:#64748b;display:block;">User Email</span>
            <span style="font-size:14px;color:#e2e8f0;">{html.escape(user_email) if user_email else 'N/A'}</span>
          </td>
        </tr>
      </table>
    </td>
  </tr>

  <!-- Error message -->
  <tr>
    <td style="padding:8px 32px 24px 32px;">
      <h2 style="margin:0 0 12px 0;font-size:13px;font-weight:600;color:#f87171;text-transform:uppercase;letter-spacing:0.08em;">
        &#10006; Error Details
      </h2>
      <div style="background-color:rgba(239,68,68,0.08);border:1px solid rgba(239,68,68,0.2);border-radius:10px;padding:16px;overflow:auto;">
        <pre style="margin:0;font-size:13px;color:#fca5a5;font-family:'SFMono-Regular',Consolas,'Liberation Mono',Menlo,monospace;white-space:pre-wrap;word-break:break-word;line-height:1.6;">{safe_error}</pre>
      </div>
    </td>
  </tr>

  <!-- CTA button -->
  <tr>
    <td style="padding:0 32px 32px 32px;">
      <table role="presentation" width="100%" cellpadding="0" cellspacing="0">
        <tr>
          <td align="center">
            <a href="{report_link}"
               style="display:inline-block;padding:12px 32px;background:linear-gradient(135deg,#6366f1,#818cf8);color:#ffffff;text-decoration:none;border-radius:10px;font-size:14px;font-weight:700;letter-spacing:0.02em;box-shadow:0 4px 12px rgba(99,102,241,0.4);">
              View Report &rarr;
            </a>
          </td>
        </tr>
      </table>
    </td>
  </tr>

  <!-- Footer -->
  <tr>
    <td style="padding:16px 32px;background-color:rgba(15,23,42,0.5);border-top:1px solid rgba(148,163,184,0.06);">
      <p style="margin:0;font-size:12px;color:#475569;text-align:center;">
        This is an automated notification from
        <span style="color:#818cf8;font-weight:600;">Klex Reporting</span>.
        You are receiving this because you are configured as a failure notification recipient.
      </p>
    </td>
  </tr>

</table>
<!-- /Card -->

</td></tr>
</table>
<!-- /Outer wrapper -->

</body>
</html>"""
