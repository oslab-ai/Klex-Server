from django.contrib import admin
from .models import User

@admin.register(User)
class UserAdmin(admin.ModelAdmin):
    list_display = ['username', 'email', 'display_name', 'is_admin', 'is_active', 'created_at']
    list_filter = ['is_admin', 'is_active']
    search_fields = ['username', 'email', 'display_name']
