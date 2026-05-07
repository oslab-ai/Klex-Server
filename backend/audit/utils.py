"""
Utility function for logging audit actions.
"""
from .models import AuditLog


def log_action(user, action, meta=None):
    """
    Create an audit log entry.
    
    Args:
        user: The user performing the action
        action: String describing the action (e.g., 'user_created', 'repo_synced')
        meta: Optional dict with additional details
    """
    AuditLog.objects.create(
        user=user,
        action=action,
        meta=meta or {}
    )
