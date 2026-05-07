"""
GitHub OAuth token storage.
"""
from django.db import models
from django.conf import settings


class GitHubToken(models.Model):
    """
    Stores GitHub OAuth access tokens for users.
    """
    user = models.OneToOneField(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='github_token'
    )
    access_token = models.CharField(max_length=255)
    token_type = models.CharField(max_length=50, default='bearer')
    scope = models.CharField(max_length=255, null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    
    class Meta:
        db_table = 'github_tokens'
    
    def __str__(self):
        return f"GitHub token for {self.user.username}"
