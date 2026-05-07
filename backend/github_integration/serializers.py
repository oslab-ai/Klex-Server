"""
Serializers for github_integration app.
"""
from rest_framework import serializers


class GitHubRepoSerializer(serializers.Serializer):
    """Serializer for GitHub repository data from API."""
    id = serializers.IntegerField()
    name = serializers.CharField()
    full_name = serializers.CharField()
    owner = serializers.SerializerMethodField()
    private = serializers.BooleanField()
    html_url = serializers.URLField()
    clone_url = serializers.URLField()
    default_branch = serializers.CharField()
    
    def get_owner(self, obj):
        return obj.get('owner', {}).get('login', '')
