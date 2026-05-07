from django.contrib import admin
from .models import AuditLog

@admin.register(AuditLog)
class AuditLogAdmin(admin.ModelAdmin):
    list_display = ['user', 'action', 'ts']
    list_filter = ['action']
    search_fields = ['action', 'user__username']
    readonly_fields = ['user', 'action', 'meta', 'ts']
