from django.contrib import admin
from .models import Repo

@admin.register(Repo)
class RepoAdmin(admin.ModelAdmin):
    list_display = ['owner', 'name', 'branch', 'path_prefix', 'last_synced_at', 'created_at']
    search_fields = ['owner', 'name']
