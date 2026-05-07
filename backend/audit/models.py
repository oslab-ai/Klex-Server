"""
AuditLog model for tracking admin actions.
"""
from django.db import models
from django.conf import settings


class AuditLog(models.Model):
    """
    Stores audit trail of admin actions.
    """
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.SET_NULL,
        null=True,
        related_name='audit_logs'
    )
    action = models.CharField(max_length=255)
    meta = models.JSONField(default=dict)
    ts = models.DateTimeField(auto_now_add=True)
    
    class Meta:
        db_table = 'audit_logs'
        ordering = ['-ts']
    
    def __str__(self):
        return f"{self.action} by {self.user.username if self.user else 'Unknown'} at {self.ts}"
