"""
Views for audit app.
"""
from rest_framework import status, generics
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response

from .models import AuditLog
from .serializers import AuditLogSerializer


class AuditLogListView(generics.ListAPIView):
    """List audit logs (admin only)."""
    serializer_class = AuditLogSerializer
    permission_classes = [IsAuthenticated]
    queryset = AuditLog.objects.all()
    
    def list(self, request, *args, **kwargs):
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )
        
        # Optional filtering by action
        action = request.query_params.get('action')
        if action:
            self.queryset = self.queryset.filter(action=action)
        
        # Optional filtering by user
        user_id = request.query_params.get('user_id')
        if user_id:
            self.queryset = self.queryset.filter(user_id=user_id)
        
        return super().list(request, *args, **kwargs)
