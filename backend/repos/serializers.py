"""
Serializers for repos app.
"""
from rest_framework import serializers
from .models import Repo


class RepoSerializer(serializers.ModelSerializer):
    """Serializer for Repo model."""
    full_name = serializers.CharField(read_only=True)
    
    class Meta:
        model = Repo
        fields = [
            'id', 'owner', 'name', 'path_prefix', 'branch',
            'last_synced_at', 'git_remote_url', 'created_at', 'full_name'
        ]
        read_only_fields = ['id', 'created_at', 'last_synced_at']


class RepoCreateSerializer(serializers.ModelSerializer):
    """Serializer for creating a repo from GitHub selection."""
    
    class Meta:
        model = Repo
        fields = ['id', 'owner', 'name', 'path_prefix', 'branch', 'git_remote_url']
        read_only_fields = ['id']
    
    def validate(self, data):
        # Check for duplicate
        if Repo.objects.filter(owner=data['owner'], name=data['name']).exists():
            raise serializers.ValidationError('This repository is already connected')
        return data

