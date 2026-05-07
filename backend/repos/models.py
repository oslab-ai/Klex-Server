"""
Repo model for connected GitHub repositories.
"""
from django.db import models
from django.conf import settings


class Repo(models.Model):
    """
    Connected GitHub repository containing reports.
    Each folder in path_prefix becomes a report.
    """
    owner = models.CharField(max_length=255)
    name = models.CharField(max_length=255)
    path_prefix = models.CharField(max_length=255, default='', blank=True)
    branch = models.CharField(max_length=255, default='main')
    last_synced_at = models.DateTimeField(null=True, blank=True)
    git_remote_url = models.URLField(max_length=500, null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    created_by = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.SET_NULL,
        null=True,
        related_name='repos'
    )
    
    class Meta:
        db_table = 'repos'
        unique_together = ['owner', 'name']
    
    def __str__(self):
        return f"{self.owner}/{self.name}"
    
    @property
    def full_name(self):
        return f"{self.owner}/{self.name}"
