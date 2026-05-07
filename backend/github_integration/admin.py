from django.contrib import admin
from .models import GitHubToken

@admin.register(GitHubToken)
class GitHubTokenAdmin(admin.ModelAdmin):
    list_display = ['user', 'scope', 'created_at', 'updated_at']
