"""
Serializers for dataadapter app.
"""
from rest_framework import serializers
from .models import DataAdapter


class DataAdapterSerializer(serializers.ModelSerializer):
    """Serializer for DataAdapter model."""
    created_by_name = serializers.CharField(source='created_by.username', read_only=True)
    
    class Meta:
        model = DataAdapter
        fields = [
            'id', 'name', 'adapter_type', 'connection_details',
            'is_active', 'created_by', 'created_by_name', 'created_at', 'updated_at'
        ]
        read_only_fields = ['id', 'created_by', 'created_at', 'updated_at']
    
    def validate_connection_details(self, value):
        """Validate connection details based on adapter type."""
        adapter_type = self.initial_data.get('adapter_type')
        
        if adapter_type == 'jdbc':
            required_fields = ['host', 'port', 'database', 'username']
            for field in required_fields:
                if field not in value:
                    raise serializers.ValidationError(f"JDBC adapter requires '{field}' in connection_details")
        
        # CSV/JSON/XML adapter: file_path is set by the upload handler, no strict validation here
        # mock/inmemory adapters don't need validation
        return value


class TestConnectionSerializer(serializers.Serializer):
    """Serializer for test connection request."""
    adapter_type = serializers.ChoiceField(choices=DataAdapter.ADAPTER_TYPE_CHOICES)
    connection_details = serializers.JSONField()
