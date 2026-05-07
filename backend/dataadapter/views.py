"""
Views for dataadapter app.
"""
import os
import psycopg2
from pathlib import Path
from django.conf import settings
from rest_framework import status, generics
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework.parsers import MultiPartParser, FormParser, JSONParser

from .models import DataAdapter
from .serializers import DataAdapterSerializer, TestConnectionSerializer
from audit.utils import log_action


class DataAdapterListView(generics.ListCreateAPIView):
    """List and create data adapters (admin only)."""
    serializer_class = DataAdapterSerializer
    permission_classes = [IsAuthenticated]
    parser_classes = [MultiPartParser, FormParser, JSONParser]
    queryset = DataAdapter.objects.all().order_by('-created_at')
    
    def list(self, request, *args, **kwargs):
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )
        return super().list(request, *args, **kwargs)
    
    def create(self, request, *args, **kwargs):
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )
        
        # Handle file-based uploads (CSV, JSON, XML)
        adapter_type = request.data.get('adapter_type')
        file_key = None
        upload_subdir = None
        
        if adapter_type == 'csv' and 'csv_file' in request.FILES:
            file_key = 'csv_file'
            upload_subdir = 'csv_uploads'
        elif adapter_type == 'json' and 'data_file' in request.FILES:
            file_key = 'data_file'
            upload_subdir = 'json_uploads'
        elif adapter_type == 'xml' and 'data_file' in request.FILES:
            file_key = 'data_file'
            upload_subdir = 'xml_uploads'
        
        if file_key and upload_subdir:
            data_file = request.FILES[file_key]
            
            # Save file to media/<upload_subdir>/
            upload_dir = Path(settings.MEDIA_ROOT) / upload_subdir
            upload_dir.mkdir(parents=True, exist_ok=True)
            
            # Create unique filename
            import uuid
            ext = os.path.splitext(data_file.name)[1] or f'.{adapter_type}'
            filename = f"{uuid.uuid4().hex[:12]}{ext}"
            file_path = upload_dir / filename
            
            with open(file_path, 'wb+') as dest:
                for chunk in data_file.chunks():
                    dest.write(chunk)
            
            # Build connection_details with file path and original name
            connection_details = {
                'file_path': str(file_path),
                'original_name': data_file.name,
                'file_size': data_file.size,
            }
            
            # Only mark active if no adapter is currently active
            has_active = DataAdapter.objects.filter(is_active=True).exists()

            name = request.data.get('name', data_file.name)
            adapter = DataAdapter.objects.create(
                name=name,
                adapter_type=adapter_type,
                connection_details=connection_details,
                is_active=not has_active,
                created_by=request.user,
            )
            
            log_action(request.user, 'data_adapter_created', {
                'adapter_name': name,
                'adapter_type': adapter_type,
                'file': data_file.name,
            })
            
            serializer = self.get_serializer(adapter)
            return Response(serializer.data, status=status.HTTP_201_CREATED)
        
        return super().create(request, *args, **kwargs)
    
    def perform_create(self, serializer):
        serializer.save(created_by=self.request.user)
        log_action(self.request.user, 'data_adapter_created', {
            'adapter_name': serializer.data.get('name'),
            'adapter_type': serializer.data.get('adapter_type'),
        })


class DataAdapterDetailView(generics.RetrieveUpdateDestroyAPIView):
    """Get, update, or delete a data adapter (admin only)."""
    serializer_class = DataAdapterSerializer
    permission_classes = [IsAuthenticated]
    queryset = DataAdapter.objects.all()
    
    def retrieve(self, request, *args, **kwargs):
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )
        return super().retrieve(request, *args, **kwargs)
    
    def update(self, request, *args, **kwargs):
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )
        response = super().update(request, *args, **kwargs)
        log_action(request.user, 'data_adapter_updated', {
            'adapter_id': kwargs.get('pk'),
        })
        return response
    
    def destroy(self, request, *args, **kwargs):
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )
        adapter = self.get_object()
        
        # Clean up uploaded file if it exists (csv, json, xml)
        if adapter.adapter_type in ('csv', 'json', 'xml'):
            file_path = adapter.connection_details.get('file_path', '')
            if file_path and os.path.exists(file_path):
                try:
                    os.remove(file_path)
                except OSError:
                    pass
        
        log_action(request.user, 'data_adapter_deleted', {
            'adapter_name': adapter.name,
        })
        return super().destroy(request, *args, **kwargs)


class TestConnectionView(APIView):
    """
    Test a data adapter connection.
    Can test both saved adapters and new connection details.
    """
    permission_classes = [IsAuthenticated]
    
    def post(self, request, pk=None):
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )
        
        if pk:
            # Test existing adapter
            try:
                adapter = DataAdapter.objects.get(pk=pk)
                adapter_type = adapter.adapter_type
                connection_details = adapter.connection_details
            except DataAdapter.DoesNotExist:
                return Response(
                    {'error': 'Adapter not found'},
                    status=status.HTTP_404_NOT_FOUND
                )
        else:
            # Test new connection details
            serializer = TestConnectionSerializer(data=request.data)
            serializer.is_valid(raise_exception=True)
            adapter_type = serializer.validated_data['adapter_type']
            connection_details = serializer.validated_data['connection_details']
        
        # Test the connection
        try:
            if adapter_type == 'jdbc':
                result = self._test_postgres_connection(connection_details)
            elif adapter_type == 'csv':
                result = self._test_csv_connection(connection_details)
            elif adapter_type == 'json':
                result = self._test_json_connection(connection_details)
            elif adapter_type == 'xml':
                result = self._test_xml_connection(connection_details)
            elif adapter_type == 'inmemory':
                result = self._test_inmemory_connection(connection_details)
            elif adapter_type == 'mock':
                result = self._test_mock_connection(connection_details)
            else:
                return Response(
                    {'error': f'Unknown adapter type: {adapter_type}'},
                    status=status.HTTP_400_BAD_REQUEST
                )
            
            return Response({
                'success': result['success'],
                'message': result['message'],
            })
        except Exception as e:
            return Response({
                'success': False,
                'message': f'Connection test failed: {str(e)}',
            })
    
    def _test_postgres_connection(self, details):
        """Test PostgreSQL connection."""
        try:
            host = details.get('host', 'localhost')
            port = details.get('port', 5432)
            database = details.get('database', 'postgres')
            username = details.get('username', 'postgres')
            password = details.get('password', '')

            jdbc_url = details.get('url', '')
            if jdbc_url:
                import re
                match = re.match(
                    r'jdbc:postgresql://([^:/]+)(?::(\d+))?/(\w+)',
                    jdbc_url
                )
                if match:
                    host = match.group(1)
                    port = int(match.group(2)) if match.group(2) else 5432
                    database = match.group(3)

            conn = psycopg2.connect(
                host=host,
                port=port,
                database=database,
                user=username,
                password=password,
                connect_timeout=5
            )
            conn.close()
            return {'success': True, 'message': 'PostgreSQL connection successful'}
        except psycopg2.Error as e:
            return {'success': False, 'message': f'PostgreSQL connection failed: {str(e)}'}
    
    def _test_csv_connection(self, details):
        file_path = details.get('file_path', '')
        if not file_path:
            return {'success': False, 'message': 'No file path specified'}
        if os.path.exists(file_path) and os.path.isfile(file_path):
            return {'success': True, 'message': 'CSV file is accessible'}
        return {'success': False, 'message': 'CSV file not found or not accessible'}
    
    def _test_mock_connection(self, details):
        return {'success': True, 'message': 'Mock connection successful'}

    def _test_json_connection(self, details):
        file_path = details.get('file_path', '')
        if not file_path:
            return {'success': False, 'message': 'No file path specified'}
        if os.path.exists(file_path) and os.path.isfile(file_path):
            return {'success': True, 'message': 'JSON file is accessible'}
        return {'success': False, 'message': 'JSON file not found or not accessible'}

    def _test_xml_connection(self, details):
        file_path = details.get('file_path', '')
        if not file_path:
            return {'success': False, 'message': 'No file path specified'}
        if os.path.exists(file_path) and os.path.isfile(file_path):
            return {'success': True, 'message': 'XML file is accessible'}
        return {'success': False, 'message': 'XML file not found or not accessible'}

    def _test_inmemory_connection(self, details):
        return {'success': True, 'message': 'In-Memory data source is ready'}

