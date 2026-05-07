from django.contrib import admin
from .models import DataAdapter

@admin.register(DataAdapter)
class DataAdapterAdmin(admin.ModelAdmin):
    list_display = ['name', 'adapter_type', 'is_active', 'created_at']
    list_filter = ['adapter_type', 'is_active']
