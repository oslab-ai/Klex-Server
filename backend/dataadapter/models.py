"""
DataAdapter model for database connection configurations.
"""
from django.db import models
from django.conf import settings


class DataAdapter(models.Model):
    """
    Stores connection configurations for data sources.
    Supports JDBC (PostgreSQL), CSV, and mock adapters.
    """
    ADAPTER_TYPE_CHOICES = [
        ('jdbc', 'JDBC (PostgreSQL)'),
        ('csv', 'CSV File'),
        ('json', 'JSON File'),
        ('xml', 'XML File'),
        ('inmemory', 'In-Memory'),
        ('mock', 'Mock Data'),
    ]
    
    name = models.CharField(max_length=255)
    adapter_type = models.CharField(max_length=20, choices=ADAPTER_TYPE_CHOICES)
    connection_details = models.JSONField(default=dict)
    is_active = models.BooleanField(default=True)
    created_by = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.SET_NULL,
        null=True,
        related_name='data_adapters'
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    
    class Meta:
        db_table = 'data_adapters'
    
    def __str__(self):
        return f"{self.name} ({self.adapter_type})"
