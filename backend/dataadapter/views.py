"""
Views for dataadapter app.
"""
import os
import re
import time
import threading
import logging
import psycopg2
import psycopg2.pool
from psycopg2 import sql as psql
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
from .cache import query_cache

logger = logging.getLogger(__name__)

# ============================================================
# Connection Pool Manager (module-level singleton)
# ============================================================

ALLOWED_AGGREGATES = {'SUM', 'COUNTA', 'COUNT', 'COUNTUNIQUE', 'AVERAGE', 'MAX', 'MIN', 'MEDIAN', 'PRODUCT', 'STDEV', 'STDEVP', 'VAR', 'VARP'}
ALLOWED_OPERATORS = {'=', '!=', '>', '<', '>=', '<=', 'IN', 'NOT IN', 'LIKE', 'IS NULL', 'IS NOT NULL'}
IDENTIFIER_PATTERN = re.compile(r'^[a-zA-Z_][a-zA-Z0-9_\s-]{0,62}$')
MAX_ROW_LIMIT = 10000000
DEFAULT_ROW_LIMIT = 1000
STATEMENT_TIMEOUT_MS = 30000


class ConnectionPoolManager:
    """Thread-safe, lazy connection pool cache keyed by DataAdapter.id."""

    _instance = None
    _lock = threading.Lock()

    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
                    cls._instance._pools = {}
                    cls._instance._pool_lock = threading.Lock()
        return cls._instance

    @staticmethod
    def _parse_connection_params(details):
        """Extract host/port/database/user/password from adapter connection_details."""
        host = details.get('host', 'localhost')
        port = int(details.get('port', 5432))
        database = details.get('database', 'postgres')
        username = details.get('username', 'postgres')
        password = details.get('password', '')

        jdbc_url = details.get('url', '')
        if jdbc_url:
            match = re.match(
                r'jdbc:postgresql://([^:/]+)(?::(\d+))?/([\w-]+)',
                jdbc_url
            )
            if match:
                host = match.group(1)
                port = int(match.group(2)) if match.group(2) else 5432
                database = match.group(3)

        return {
            'host': host,
            'port': port,
            'database': database,
            'user': username,
            'password': password,
        }

    def get_connection(self, adapter):
        """Acquire a connection from the pool for the given adapter."""
        adapter_id = adapter.id
        with self._pool_lock:
            if adapter_id not in self._pools:
                params = self._parse_connection_params(adapter.connection_details)
                self._pools[adapter_id] = psycopg2.pool.ThreadedConnectionPool(
                    minconn=1,
                    maxconn=10,
                    connect_timeout=10,
                    **params
                )
        return self._pools[adapter_id].getconn()

    def release_connection(self, adapter_id, conn):
        """Return a connection back to the pool."""
        with self._pool_lock:
            pool = self._pools.get(adapter_id)
            if pool:
                try:
                    pool.putconn(conn)
                except Exception:
                    pass

    def close_pool(self, adapter_id):
        """Destroy the connection pool for a given adapter."""
        with self._pool_lock:
            pool = self._pools.pop(adapter_id, None)
            if pool:
                try:
                    pool.closeall()
                except Exception:
                    pass


pool_manager = ConnectionPoolManager()


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
        # Invalidate cached connection pool and query cache for this adapter
        pool_manager.close_pool(kwargs.get('pk'))
        query_cache.invalidate_adapter(kwargs.get('pk'))
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
        
        # Invalidate cached connection pool and query cache for this adapter
        pool_manager.close_pool(adapter.id)
        query_cache.invalidate_adapter(adapter.id)
        
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


# ============================================================
# Data Exploration Views
# ============================================================

def _validate_identifier(name, label='identifier'):
    """Validate that a name is a safe SQL identifier."""
    if not name or not IDENTIFIER_PATTERN.match(name):
        raise ValueError(
            f"Invalid {label}: '{name}'. "
            f"Must be 1-63 alphanumeric, space, hyphen, or underscore characters, starting with a letter or underscore."
        )
    return name


def _get_adapter_or_error(pk, request):
    """Fetch adapter, validate admin access and jdbc/csv type."""
    if not request.user.is_admin:
        return None, Response(
            {'error': 'Admin access required'},
            status=status.HTTP_403_FORBIDDEN
        )
    try:
        adapter = DataAdapter.objects.get(pk=pk)
    except DataAdapter.DoesNotExist:
        return None, Response(
            {'error': 'Adapter not found'},
            status=status.HTTP_404_NOT_FOUND
        )
    if adapter.adapter_type not in ('jdbc', 'csv'):
        return None, Response(
            {'error': 'Data exploration is only supported for PostgreSQL (JDBC) and CSV adapters.'},
            status=status.HTTP_400_BAD_REQUEST
        )
    return adapter, None


class DataAdapterTablesView(APIView):
    """Discover public tables and views for a PostgreSQL or CSV adapter."""
    permission_classes = [IsAuthenticated]

    def get(self, request, pk):
        adapter, error = _get_adapter_or_error(pk, request)
        if error:
            return error

        if adapter.adapter_type == 'csv':
            table_name = adapter.connection_details.get('original_name', 'csv_data')
            # Strip file extension if present to make it look like a clean table name
            table_name = os.path.splitext(table_name)[0]
            return Response({'tables': [table_name]})

        conn = None
        try:
            conn = pool_manager.get_connection(adapter)
            with conn.cursor() as cursor:
                cursor.execute(
                    "SELECT table_name FROM information_schema.tables "
                    "WHERE table_schema = 'public' "
                    "AND table_type IN ('BASE TABLE', 'VIEW') "
                    "ORDER BY table_name"
                )
                tables = [row[0] for row in cursor.fetchall()]
            return Response({'tables': tables})
        except psycopg2.Error as e:
            logger.exception('Failed to list tables for adapter %s', pk)
            return Response(
                {'error': f'Database error: {str(e)}'},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )
        finally:
            if conn:
                pool_manager.release_connection(pk, conn)


class DataAdapterColumnsView(APIView):
    """Retrieve column metadata for a table in a PostgreSQL or CSV adapter."""
    permission_classes = [IsAuthenticated]

    def get(self, request, pk):
        adapter, error = _get_adapter_or_error(pk, request)
        if error:
            return error

        table_name = request.query_params.get('table', '').strip()
        try:
            _validate_identifier(table_name, 'table name')
        except ValueError as e:
            return Response({'error': str(e)}, status=status.HTTP_400_BAD_REQUEST)

        if adapter.adapter_type == 'csv':
            file_path = adapter.connection_details.get('file_path')
            if not file_path or not os.path.exists(file_path):
                return Response({'error': 'CSV file not found.'}, status=status.HTTP_404_NOT_FOUND)
            
            try:
                import csv
                with open(file_path, 'r', encoding='utf-8-sig') as f:
                    reader = csv.reader(f)
                    headers = next(reader, [])
                    
                    # Read up to 5 rows to detect datatypes
                    rows = []
                    for _ in range(5):
                        try:
                            row = next(reader)
                            rows.append(row)
                        except StopIteration:
                            break
                
                columns = []
                for i, header in enumerate(headers):
                    if not header or not header.strip():
                        header = f"column_{i+1}"
                    else:
                        header = header.strip()
                    # Detect datatype
                    is_num = True
                    for r in rows:
                        if i < len(r) and r[i].strip():
                            try:
                                float(r[i])
                            except ValueError:
                                is_num = False
                                break
                    data_type = 'numeric' if (rows and is_num) else 'varchar'
                    columns.append({
                        'name': header,
                        'data_type': data_type,
                        'nullable': True
                    })
                return Response({'columns': columns})
            except Exception as e:
                return Response({'error': f'Failed to parse CSV: {str(e)}'}, status=status.HTTP_400_BAD_REQUEST)

        conn = None
        try:
            conn = pool_manager.get_connection(adapter)
            with conn.cursor() as cursor:
                cursor.execute(
                    "SELECT column_name, data_type, is_nullable "
                    "FROM information_schema.columns "
                    "WHERE table_schema = 'public' AND table_name = %s "
                    "ORDER BY ordinal_position",
                    [table_name]
                )
                columns = [
                    {
                        'name': row[0],
                        'data_type': row[1],
                        'nullable': row[2] == 'YES',
                    }
                    for row in cursor.fetchall()
                ]
            if not columns:
                return Response(
                    {'error': f"Table '{table_name}' not found or has no columns."},
                    status=status.HTTP_404_NOT_FOUND
                )
            return Response({'columns': columns})
        except psycopg2.Error as e:
            logger.exception('Failed to list columns for adapter %s, table %s', pk, table_name)
            return Response(
                {'error': f'Database error: {str(e)}'},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )
        finally:
            if conn:
                pool_manager.release_connection(pk, conn)


class DataAdapterDistinctValuesView(APIView):
    """Retrieve distinct values for a column in a table (for filter checklists)."""
    permission_classes = [IsAuthenticated]

    DISTINCT_VALUES_LIMIT = 500

    def get(self, request, pk):
        adapter, error = _get_adapter_or_error(pk, request)
        if error:
            return error

        table_name = request.query_params.get('table', '').strip()
        column_name = request.query_params.get('column', '').strip()
        try:
            _validate_identifier(table_name, 'table name')
            _validate_identifier(column_name, 'column name')
        except ValueError as e:
            return Response({'error': str(e)}, status=status.HTTP_400_BAD_REQUEST)

        from opentelemetry import trace
        tracer = trace.get_tracer("klex.dataadapter")

        with tracer.start_as_current_span("distinct_values_query") as span:
            span.set_attribute("query.adapter_id", pk)
            span.set_attribute("query.adapter_name", adapter.name)
            span.set_attribute("query.adapter_type", adapter.adapter_type)
            span.set_attribute("query.table_name", table_name)
            span.set_attribute("query.column_name", column_name)
            span.set_attribute("user.id", str(request.user.id))
            span.set_attribute("user.username", request.user.username)

            if adapter.adapter_type == 'csv':
                span.set_attribute("query.engine", "csv")
                import csv as csv_mod
                file_path = adapter.connection_details.get('file_path')
                if not file_path or not os.path.exists(file_path):
                    span.set_status(trace.StatusCode.ERROR, description="CSV file not found")
                    return Response({'error': 'CSV file not found.'}, status=status.HTTP_404_NOT_FOUND)
                try:
                    logger.info(f"Query engine: CSV. Retrieving distinct values for column '{column_name}' in table '{table_name}'. File: {file_path}")
                    span.set_attribute("query.file_path", file_path)
                    unique_vals = set()
                    with open(file_path, 'r', encoding='utf-8-sig') as f:
                        reader = csv_mod.DictReader(f)
                        for row in reader:
                            val = row.get(column_name)
                            if val is None:
                                val = row.get(column_name.strip())
                            if val is not None:
                                val = str(val).strip()
                                if val:
                                    unique_vals.add(val)
                            if len(unique_vals) >= self.DISTINCT_VALUES_LIMIT:
                                break
                    span.set_attribute("query.row_count", len(unique_vals))
                    return Response({'values': sorted(unique_vals)})
                except Exception as e:
                    logger.exception('Failed to read CSV for distinct values')
                    span.record_exception(e)
                    span.set_status(trace.StatusCode.ERROR, description=str(e))
                    return Response({'error': f'Failed to read CSV: {str(e)}'}, status=status.HTTP_400_BAD_REQUEST)

            span.set_attribute("query.engine", "postgresql")
            conn = None
            try:
                conn = pool_manager.get_connection(adapter)
                with conn.cursor() as cursor:
                    query = psql.SQL(
                        'SELECT DISTINCT {} FROM {} WHERE {} IS NOT NULL ORDER BY {} LIMIT {}'
                    ).format(
                        psql.Identifier(column_name),
                        psql.Identifier(table_name),
                        psql.Identifier(column_name),
                        psql.Identifier(column_name),
                        psql.Literal(self.DISTINCT_VALUES_LIMIT),
                    )
                    sql_string = query.as_string(conn)
                    span.set_attribute("db.statement", sql_string)
                    logger.info(f"Query engine: PostgreSQL. Retrieving distinct values for column '{column_name}' in table '{table_name}'. SQL: {sql_string}")
                    cursor.execute(query)
                    values = [str(row[0]) for row in cursor.fetchall()]
                span.set_attribute("query.row_count", len(values))
                return Response({'values': values})
            except psycopg2.Error as e:
                logger.exception('Failed to get distinct values for adapter %s', pk)
                span.record_exception(e)
                span.set_status(trace.StatusCode.ERROR, description=str(e))
                return Response(
                    {'error': f'Database error: {str(e)}'},
                    status=status.HTTP_500_INTERNAL_SERVER_ERROR
                )
            finally:
                if conn:
                    pool_manager.release_connection(pk, conn)


class DataAdapterQueryView(APIView):
    """
    Execute a dynamic, validated SQL query against a PostgreSQL adapter.
    Builds secure SQL using psycopg2.sql with strict allow-lists.
    """
    permission_classes = [IsAuthenticated]

    def post(self, request, pk):
        adapter, error = _get_adapter_or_error(pk, request)
        if error:
            return error

        payload = request.data
        cached_data = query_cache.get(pk, payload)
        if cached_data is not None:
            logger.info(f"Query cache HIT for adapter {pk} with payload: {payload}")
            response_data = {
                'data': cached_data['data'],
                'row_count': cached_data['row_count'],
                'columns': cached_data['columns'],
                'query_time_ms': 0.0,
                'cache_hit': True,
            }
            if getattr(request.user, 'is_super_admin', False):
                if adapter.adapter_type == 'csv':
                    file_path = adapter.connection_details.get('file_path', '')
                    csv_query_desc = self._build_csv_query_description(file_path, payload)
                    response_data['debug'] = {
                        'generated_query': csv_query_desc,
                        'query_params': [],
                        'query_engine': 'csv',
                    }
                else:
                    try:
                        if payload.get('joins'):
                            query, params, _ = self._build_safe_join_query(payload)
                        else:
                            query, params, _ = self._build_safe_query(payload)
                        conn = pool_manager.get_connection(adapter)
                        try:
                            sql_string = query.as_string(conn)
                            response_data['debug'] = {
                                'generated_query': self._build_readable_query(sql_string, params),
                                'query_params': [str(p) for p in params],
                                'query_engine': 'postgresql',
                            }
                        finally:
                            pool_manager.release_connection(pk, conn)
                    except Exception as e:
                        logger.warning(f"Could not generate debug query for cache hit: {e}")
            return Response(response_data)

        from opentelemetry import trace
        tracer = trace.get_tracer("klex.dataadapter")

        with tracer.start_as_current_span("data_exploration_query") as span:
            span.set_attribute("query.adapter_id", pk)
            span.set_attribute("query.adapter_name", adapter.name)
            span.set_attribute("query.adapter_type", adapter.adapter_type)
            span.set_attribute("query.table_name", request.data.get('table_name', ''))
            span.set_attribute("user.id", str(request.user.id))
            span.set_attribute("user.username", request.user.username)

            if request.data.get('row_limit') is not None:
                span.set_attribute("query.row_limit", int(request.data.get('row_limit')))
            
            if adapter.adapter_type == 'csv':
                file_path = adapter.connection_details.get('file_path')
                if not file_path or not os.path.exists(file_path):
                    span.set_status(trace.StatusCode.ERROR, description="CSV file not found")
                    return Response({'error': 'CSV file not found on server.'}, status=status.HTTP_404_NOT_FOUND)
                
                # Check path safety
                if not Path(file_path).resolve().is_relative_to(Path(settings.MEDIA_ROOT).resolve()):
                    span.set_status(trace.StatusCode.ERROR, description="Access denied")
                    return Response({'error': 'Access denied: invalid file path.'}, status=status.HTTP_403_FORBIDDEN)
                
                payload = request.data
                try:
                    logger.info(f"Query engine: CSV. Formulating query for file: {file_path}. Payload: {payload}")
                    print(f"\n--- [CSV QUERY EXECUTION START] ---")
                    print(f"File: {file_path}")
                    print(f"Payload: {payload}")
                    
                    span.set_attribute("query.engine", "csv")
                    span.set_attribute("query.file_path", file_path)
                    span.set_attribute("query.dimensions", payload.get('dimensions', []))
                    span.set_attribute("query.metrics", [str(m) for m in payload.get('metrics', [])])
                    span.set_attribute("query.filters", [str(f) for f in payload.get('filters', [])])

                    start = time.time()
                    result_data, columns = self._execute_csv_query(file_path, payload)
                    elapsed_ms = round((time.time() - start) * 1000, 1)
                    
                    logger.info(f"Query engine: CSV. Execution completed in {elapsed_ms}ms with row count: {len(result_data)}")
                    print(f"Completed in: {elapsed_ms}ms")
                    print(f"Row count: {len(result_data)}")
                    print(f"--- [CSV QUERY EXECUTION END] ---\n")

                    span.set_attribute("query.row_count", len(result_data))
                    span.set_attribute("query.time_ms", elapsed_ms)

                    log_action(request.user, 'data_exploration_query', {
                        'adapter_id': pk,
                        'table': payload.get('table_name'),
                        'row_count': len(result_data),
                        'query_time_ms': elapsed_ms,
                    })
                    
                    response_data = {
                        'data': result_data,
                        'row_count': len(result_data),
                        'columns': columns,
                        'query_time_ms': elapsed_ms,
                    }

                    # Include debug query info for super admins
                    if getattr(request.user, 'is_super_admin', False):
                        csv_query_desc = self._build_csv_query_description(file_path, payload)
                        response_data['debug'] = {
                            'generated_query': csv_query_desc,
                            'query_params': [],
                            'query_engine': 'csv',
                        }

                    # Cache the query result
                    query_cache.set(pk, payload, {
                        'data': result_data,
                        'row_count': len(result_data),
                        'columns': columns
                    })

                    return Response(response_data)
                except Exception as e:
                    logger.exception('CSV query execution failed')
                    span.record_exception(e)
                    span.set_status(trace.StatusCode.ERROR, description=str(e))
                    return Response({'error': f'CSV query error: {str(e)}'}, status=status.HTTP_400_BAD_REQUEST)

            payload = request.data
            try:
                if payload.get('joins'):
                    logger.info(f"Query engine: PostgreSQL (multi-table). Building SQL query with joins. Payload: {payload}")
                    query, params, select_aliases = self._build_safe_join_query(payload)
                else:
                    logger.info(f"Query engine: PostgreSQL (single-table). Building SQL query. Payload: {payload}")
                    query, params, select_aliases = self._build_safe_query(payload)
            except ValueError as e:
                span.set_status(trace.StatusCode.ERROR, description=str(e))
                return Response({'error': str(e)}, status=status.HTTP_400_BAD_REQUEST)

            span.set_attribute("query.engine", "postgresql")
            span.set_attribute("query.dimensions", payload.get('dimensions', []))
            span.set_attribute("query.metrics", [str(m) for m in payload.get('metrics', [])])
            span.set_attribute("query.filters", [str(f) for f in payload.get('filters', [])])

            conn = None
            try:
                conn = pool_manager.get_connection(adapter)
                
                # Print/Log compiled SQL query with actual parameters
                sql_string = query.as_string(conn)
                span.set_attribute("db.statement", sql_string)
                span.set_attribute("db.params", str(params))

                logger.info(f"Query engine: PostgreSQL. Executing SQL: {sql_string} with params: {params}")
                print(f"\n--- [SQL QUERY EXECUTION START] ---")
                print(f"SQL: {sql_string}")
                print(f"Params: {params}")

                start = time.time()
                with conn.cursor() as cursor:
                    cursor.execute(
                        psql.SQL("SET statement_timeout = {}").format(
                            psql.Literal(STATEMENT_TIMEOUT_MS)
                        )
                    )
                    cursor.execute(query, params)
                    columns = [desc[0] for desc in cursor.description]
                    rows = cursor.fetchall()
                elapsed_ms = round((time.time() - start) * 1000, 1)
                
                logger.info(f"Query engine: PostgreSQL. Execution completed in {elapsed_ms}ms with row count: {len(rows)}")
                print(f"Completed in: {elapsed_ms}ms")
                print(f"Row count: {len(rows)}")
                print(f"--- [SQL QUERY EXECUTION END] ---\n")

                result_data = [dict(zip(columns, row)) for row in rows]
                
                span.set_attribute("query.row_count", len(result_data))
                span.set_attribute("query.time_ms", elapsed_ms)

                log_action(request.user, 'data_exploration_query', {
                    'adapter_id': pk,
                    'table': payload.get('table_name'),
                    'row_count': len(result_data),
                    'query_time_ms': elapsed_ms,
                })

                response_data = {
                    'data': result_data,
                    'row_count': len(result_data),
                    'columns': columns,
                    'query_time_ms': elapsed_ms,
                }

                # Include debug query info for super admins
                if getattr(request.user, 'is_super_admin', False):
                    response_data['debug'] = {
                        'generated_query': self._build_readable_query(sql_string, params),
                        'query_params': [str(p) for p in params],
                        'query_engine': 'postgresql',
                    }

                # Cache the query result
                query_cache.set(pk, payload, {
                    'data': result_data,
                    'row_count': len(result_data),
                    'columns': columns
                })

                return Response(response_data)
            except psycopg2.OperationalError as e:
                err_msg = str(e).strip()
                logger.error(f"Database connection error for adapter {pk}: {err_msg}")
                span.record_exception(e)
                span.set_status(trace.StatusCode.ERROR, description=err_msg)
                suggestion = "Check your database connection settings and credentials."
                if "timeout" in err_msg.lower():
                    suggestion = "Connection timed out. Check if the database host is reachable."
                elif "password authentication failed" in err_msg.lower() or "ident authentication failed" in err_msg.lower():
                    suggestion = "Database authentication failed. Verify your username and password."
                elif "does not exist" in err_msg.lower():
                    suggestion = "Database does not exist or target server is offline."
                return Response(
                    {'error': f"Database connection error: {err_msg}. {suggestion}"},
                    status=status.HTTP_500_INTERNAL_SERVER_ERROR
                )
            except psycopg2.InterfaceError as e:
                err_msg = str(e).strip()
                logger.error(f"Database interface error for adapter {pk}: {err_msg}")
                span.record_exception(e)
                span.set_status(trace.StatusCode.ERROR, description=err_msg)
                return Response(
                    {'error': f"Client not connected or connection closed: {err_msg}. Please retry."},
                    status=status.HTTP_500_INTERNAL_SERVER_ERROR
                )
            except psycopg2.Error as e:
                logger.exception('Query execution failed for adapter %s', pk)
                span.record_exception(e)
                span.set_status(trace.StatusCode.ERROR, description=str(e))
                return Response(
                    {'error': f'Query execution error: {str(e)}'},
                    status=status.HTTP_500_INTERNAL_SERVER_ERROR
                )
            except Exception as e:
                logger.exception('Unexpected error during query execution for adapter %s', pk)
                span.record_exception(e)
                span.set_status(trace.StatusCode.ERROR, description=str(e))
                return Response(
                    {'error': f'Unexpected query error: {str(e)}'},
                    status=status.HTTP_500_INTERNAL_SERVER_ERROR
                )
            finally:
                if conn:
                    try:
                        conn.rollback()  # reset statement_timeout / any txn state
                    except Exception:
                        pass
                    pool_manager.release_connection(pk, conn)

    def _execute_csv_query(self, file_path, payload):
        import csv
        
        dimensions = payload.get('dimensions', [])
        metrics = payload.get('metrics', [])
        filters = payload.get('filters', [])
        row_limit = payload.get('row_limit')
        if row_limit is not None:
            try:
                row_limit = min(int(row_limit), MAX_ROW_LIMIT)
            except (ValueError, TypeError):
                row_limit = None
        
        # Load CSV data
        raw_rows = []
        with open(file_path, 'r', encoding='utf-8-sig') as f:
            reader = csv.DictReader(f)
            for row in reader:
                clean_row = {}
                for k, v in row.items():
                    if k is not None:
                        clean_row[k.strip()] = v.strip() if v is not None else ''
                raw_rows.append(clean_row)
                
        # Validate parameters
        for dim in dimensions:
            _validate_identifier(dim, 'dimension')
        for metric in metrics:
            col = metric.get('column', '')
            agg = metric.get('aggregate', '').upper()
            if agg == 'COUNT_DISTINCT':
                agg = 'COUNTUNIQUE'
                metric['aggregate'] = 'COUNTUNIQUE'
            elif agg == 'AVG':
                agg = 'AVERAGE'
                metric['aggregate'] = 'AVERAGE'
            alias = metric.get('alias', '')
            _validate_identifier(col, 'metric column')
            if alias:
                _validate_identifier(alias, 'metric alias')
            if agg not in ALLOWED_AGGREGATES:
                raise ValueError(f"Aggregate '{agg}' is not allowed.")
        for f in filters:
            _validate_identifier(f.get('column', ''), 'filter column')
            if f.get('operator', '').upper() not in ALLOWED_OPERATORS:
                raise ValueError(f"Filter operator '{f.get('operator')}' is not allowed.")
                
        # Filter helper
        def match_filter(row_val, op, filter_val):
            op = op.upper()
            if op == 'IS NULL':
                return row_val is None or str(row_val).strip() == ''
            if op == 'IS NOT NULL':
                return row_val is not None and str(row_val).strip() != ''
            
            try:
                num_row = float(row_val)
                num_filt = float(filter_val)
                is_numeric = True
            except (ValueError, TypeError):
                is_numeric = False
                
            if is_numeric:
                if op == '=': return num_row == num_filt
                if op == '!=': return num_row != num_filt
                if op == '>': return num_row > num_filt
                if op == '<': return num_row < num_filt
                if op == '>=': return num_row >= num_filt
                if op == '<=': return num_row <= num_filt
            
            s_row = str(row_val).strip() if row_val is not None else ''
            s_filt = str(filter_val).strip()
            
            if op == '=': return s_row.lower() == s_filt.lower()
            if op == '!=': return s_row.lower() != s_filt.lower()
            if op == 'LIKE': return s_filt.lower() in s_row.lower()
            if op == 'IN':
                return s_row in [v.strip() for v in s_filt.split(',')]
            if op == 'NOT IN':
                return s_row not in [v.strip() for v in s_filt.split(',')]
            return False

        # Apply filters
        filtered_rows = []
        for row in raw_rows:
            match = True
            for f in filters:
                col = f.get('column')
                op = f.get('operator')
                val = f.get('value')
                row_val = row.get(col)
                if not match_filter(row_val, op, val):
                    match = False
                    break
            if match:
                filtered_rows.append(row)

        # Apply Row Limit to raw rows before grouping/aggregation
        if row_limit is not None:
            filtered_rows = filtered_rows[:row_limit]

        # Aggregate helper
        def aggregate_vals(rows, col, agg):
            if agg == 'COUNTA':
                return len([r for r in rows if r.get(col) is not None and str(r.get(col)).strip() != ''])
            if agg == 'COUNT':
                count = 0
                for r in rows:
                    val = r.get(col)
                    if val is not None and str(val).strip() != '':
                        try:
                            float(val)
                            count += 1
                        except (ValueError, TypeError):
                            pass
                return count
            if agg == 'COUNTUNIQUE':
                return len(set(str(r.get(col)) for r in rows if r.get(col) is not None and str(r.get(col)).strip() != ''))
            
            nums = []
            for r in rows:
                val = r.get(col)
                if val is not None and str(val).strip() != '':
                    try:
                        nums.append(float(val))
                    except (ValueError, TypeError):
                        pass
            if not nums:
                return 0 if agg in ('SUM', 'AVERAGE') else None
            
            if agg == 'SUM':
                return sum(nums)
            if agg == 'AVERAGE':
                return sum(nums) / len(nums)
            if agg == 'MIN':
                return min(nums)
            if agg == 'MAX':
                return max(nums)
            if agg == 'MEDIAN':
                sorted_nums = sorted(nums)
                n = len(sorted_nums)
                mid = n // 2
                if n % 2 == 0:
                    return (sorted_nums[mid - 1] + sorted_nums[mid]) / 2
                return sorted_nums[mid]
            if agg == 'PRODUCT':
                result = 1.0
                for num in nums:
                    result *= num
                return result
            if agg == 'STDEV':
                if len(nums) <= 1: return 0
                mean = sum(nums) / len(nums)
                variance = sum((x - mean) ** 2 for x in nums) / (len(nums) - 1)
                return variance ** 0.5
            if agg == 'STDEVP':
                if not nums: return 0
                mean = sum(nums) / len(nums)
                variance = sum((x - mean) ** 2 for x in nums) / len(nums)
                return variance ** 0.5
            if agg == 'VAR':
                if len(nums) <= 1: return 0
                mean = sum(nums) / len(nums)
                return sum((x - mean) ** 2 for x in nums) / (len(nums) - 1)
            if agg == 'VARP':
                if not nums: return 0
                mean = sum(nums) / len(nums)
                return sum((x - mean) ** 2 for x in nums) / len(nums)
            return None

        # Group By execution
        result_data = []
        if dimensions:
            groups = {}
            for row in filtered_rows:
                g_key = tuple(row.get(dim, '') for dim in dimensions)
                if g_key not in groups:
                    groups[g_key] = []
                groups[g_key].append(row)
                
            for g_key, group_rows in groups.items():
                res_row = {}
                for idx, dim in enumerate(dimensions):
                    res_row[dim] = g_key[idx]
                for metric in metrics:
                    alias = metric.get('alias') or f"{metric.get('aggregate').lower()}_{metric.get('column')}"
                    res_row[alias] = aggregate_vals(group_rows, metric.get('column'), metric.get('aggregate'))
                result_data.append(res_row)
        else:
            res_row = {}
            for metric in metrics:
                alias = metric.get('alias') or f"{metric.get('aggregate').lower()}_{metric.get('column')}"
                res_row[alias] = aggregate_vals(filtered_rows, metric.get('column'), metric.get('aggregate'))
            result_data.append(res_row)

        # headers
        output_columns = list(dimensions)
        for metric in metrics:
            alias = metric.get('alias') or f"{metric.get('aggregate').lower()}_{metric.get('column')}"
            if alias not in output_columns:
                output_columns.append(alias)

        return result_data, output_columns

    # ------------------------------------------------------------------
    # Secure query builder
    # ------------------------------------------------------------------

    def _build_safe_query(self, payload):
        """
        Compile a validated SELECT ... GROUP BY query from the
        exploration payload, wrapping the table in a limited subquery.
        Returns (query: sql.Composed, params: list, aliases: list).
        """
        table_name = payload.get('table_name', '')
        dimensions = payload.get('dimensions', [])
        metrics = payload.get('metrics', [])
        filters = payload.get('filters', [])
        row_limit = payload.get('row_limit')
        if row_limit is not None:
            try:
                row_limit = min(int(row_limit), MAX_ROW_LIMIT)
            except (ValueError, TypeError):
                row_limit = None

        if not table_name:
            raise ValueError('table_name is required.')
        _validate_identifier(table_name, 'table_name')

        if not dimensions and not metrics:
            raise ValueError('At least one dimension or metric is required.')

        select_parts = []
        group_by_parts = []
        aliases = []

        # --- dimensions ---
        for dim in dimensions:
            _validate_identifier(dim, 'dimension')
            ident = psql.Identifier(dim)
            select_parts.append(ident)
            group_by_parts.append(ident)
            aliases.append(dim)

        # --- metrics ---
        for metric in metrics:
            col = metric.get('column', '')
            agg = metric.get('aggregate', '').upper()
            alias = metric.get('alias', '')

            _validate_identifier(col, 'metric column')
            if alias:
                _validate_identifier(alias, 'metric alias')
            else:
                alias = f"{agg.lower()}_{col}"

            if agg == 'COUNT_DISTINCT':
                agg = 'COUNTUNIQUE'
            elif agg == 'AVG':
                agg = 'AVERAGE'

            if agg not in ALLOWED_AGGREGATES:
                raise ValueError(
                    f"Aggregate '{agg}' is not allowed. "
                    f"Allowed: {', '.join(sorted(ALLOWED_AGGREGATES))}"
                )

            col_ident = psql.Identifier(col)
            alias_ident = psql.Identifier(alias)

            if agg == 'COUNTUNIQUE':
                expr = psql.SQL('COUNT(DISTINCT {}) AS {}').format(col_ident, alias_ident)
            elif agg == 'COUNTA':
                expr = psql.SQL('COUNT({}) AS {}').format(col_ident, alias_ident)
            elif agg == 'COUNT':
                expr = psql.SQL("SUM(CASE WHEN {} IS NOT NULL AND {}::text ~ '^-?[0-9]+(\\.[0-9]+)?$' THEN 1 ELSE 0 END) AS {}").format(col_ident, col_ident, alias_ident)
            elif agg == 'AVERAGE':
                expr = psql.SQL('AVG({}) AS {}').format(col_ident, alias_ident)
            elif agg == 'MEDIAN':
                expr = psql.SQL('PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY {}) AS {}').format(col_ident, alias_ident)
            elif agg == 'PRODUCT':
                expr = psql.SQL('EXP(SUM(LN(NULLIF({}, 0)))) AS {}').format(col_ident, alias_ident)
            elif agg == 'STDEV':
                expr = psql.SQL('STDDEV_SAMP({}) AS {}').format(col_ident, alias_ident)
            elif agg == 'STDEVP':
                expr = psql.SQL('STDDEV_POP({}) AS {}').format(col_ident, alias_ident)
            elif agg == 'VAR':
                expr = psql.SQL('VAR_SAMP({}) AS {}').format(col_ident, alias_ident)
            elif agg == 'VARP':
                expr = psql.SQL('VAR_POP({}) AS {}').format(col_ident, alias_ident)
            else:
                agg_sql = psql.SQL(agg)
                expr = psql.SQL('{}({}) AS {}').format(agg_sql, col_ident, alias_ident)

            select_parts.append(expr)
            aliases.append(alias)

        # --- build subquery over raw limited matching rows ---
        subquery = psql.SQL('SELECT * FROM {}').format(psql.Identifier(table_name))
        params = []
        if filters:
            where_parts = []
            for f in filters:
                f_col = f.get('column', '')
                f_op = f.get('operator', '').upper()
                f_val = f.get('value', '')

                _validate_identifier(f_col, 'filter column')
                if f_op not in ALLOWED_OPERATORS:
                    raise ValueError(
                        f"Filter operator '{f_op}' is not allowed. "
                        f"Allowed: {', '.join(sorted(ALLOWED_OPERATORS))}"
                    )

                col_ident = psql.Identifier(f_col)

                if f_op in ('IS NULL', 'IS NOT NULL'):
                    where_parts.append(
                        psql.SQL('{} {}').format(col_ident, psql.SQL(f_op))
                    )
                elif f_op in ('IN', 'NOT IN'):
                    values = [v.strip() for v in str(f_val).split(',') if v.strip()]
                    if not values:
                        raise ValueError(f"IN / NOT IN filter for '{f_col}' requires at least one value.")
                    placeholders = psql.SQL(', ').join([psql.Placeholder()] * len(values))
                    where_parts.append(
                        psql.SQL('{} {} ({})').format(col_ident, psql.SQL(f_op), placeholders)
                    )
                    params.extend(values)
                else:
                    where_parts.append(
                        psql.SQL('{} {} {}').format(col_ident, psql.SQL(f_op), psql.Placeholder())
                    )
                    params.append(f_val)

            subquery = psql.SQL('{} WHERE {}').format(
                subquery,
                psql.SQL(' AND ').join(where_parts),
            )

        # Apply Row Limit to the raw dataset subquery
        if row_limit is not None:
            subquery = psql.SQL('{} LIMIT {}').format(subquery, psql.Literal(row_limit))

        # --- main query over limited raw dataset ---
        query = psql.SQL('SELECT {fields} FROM ({sub}) AS raw_limited').format(
            fields=psql.SQL(', ').join(select_parts),
            sub=subquery,
        )

        # --- GROUP BY ---
        if group_by_parts:
            query = psql.SQL('{} GROUP BY {}').format(
                query,
                psql.SQL(', ').join(group_by_parts),
            )

        return query, params, aliases

    # ------------------------------------------------------------------
    # CSV query description (for super-admin query inspector)
    # ------------------------------------------------------------------

    @staticmethod
    def _build_csv_query_description(file_path, payload):
        """Build a human-readable pseudo-SQL description for a CSV query."""
        dimensions = payload.get('dimensions', [])
        metrics = payload.get('metrics', [])
        filters = payload.get('filters', [])
        row_limit = payload.get('row_limit')
        table_name = payload.get('table_name', os.path.basename(file_path))

        parts = ['SELECT']

        # SELECT clause
        select_items = []
        for dim in dimensions:
            select_items.append(f'  {dim}')
        for m in metrics:
            col = m.get('column', '?')
            agg = m.get('aggregate', 'SUM')
            alias = m.get('alias', f'{agg.lower()}_{col}')
            select_items.append(f'  {agg}({col}) AS {alias}')
        if not select_items:
            select_items.append('  *')
        parts.append(',\n'.join(select_items))

        # FROM clause
        parts.append(f'FROM "{table_name}" (CSV: {file_path})')

        # WHERE clause
        if filters:
            where_clauses = []
            for f in filters:
                f_col = f.get('column', '?')
                f_op = f.get('operator', '=')
                f_val = f.get('value', '')
                if f_op.upper() in ('IS NULL', 'IS NOT NULL'):
                    where_clauses.append(f'  {f_col} {f_op.upper()}')
                else:
                    where_clauses.append(f"  {f_col} {f_op} '{f_val}'")
            parts.append('WHERE\n' + '\n  AND '.join(where_clauses))

        # GROUP BY clause
        if dimensions:
            parts.append(f'GROUP BY {", ".join(dimensions)}')

        # LIMIT clause
        if row_limit is not None:
            parts.append(f'LIMIT {row_limit}')

        return '\n'.join(parts)

    # ------------------------------------------------------------------
    # Readable query builder (inline params + pretty-print)
    # ------------------------------------------------------------------

    @staticmethod
    def _build_readable_query(sql_string, params):
        """
        Build a human-readable SQL query by substituting %s placeholders
        with actual parameter values and formatting it nicely.
        """
        readable = sql_string

        # Substitute %s placeholders with quoted literal values
        for param in params:
            # Escape single quotes within the param value
            escaped = str(param).replace("'", "''")
            readable = readable.replace('%s', f"'{escaped}'", 1)

        # Pretty-print: add newlines before major SQL keywords
        keywords = [
            'SELECT ', 'FROM ', 'WHERE ', 'GROUP BY ',
            'ORDER BY ', 'LIMIT ', 'HAVING ',
            'INNER JOIN ', 'LEFT JOIN ', 'RIGHT JOIN ',
            'FULL JOIN ', 'CROSS JOIN ',
        ]
        for kw in keywords:
            # Only break on top-level keywords (not inside subqueries)
            readable = readable.replace(kw, f'\n{kw}')

        # Indent SELECT list items: split commas in SELECT clause
        lines = readable.split('\n')
        formatted_lines = []
        for line in lines:
            stripped = line.strip()
            if not stripped:
                continue
            formatted_lines.append(stripped)
        readable = '\n'.join(formatted_lines)

        return readable.strip()

    # ------------------------------------------------------------------
    # Multi-table JOIN query builder
    # ------------------------------------------------------------------

    ALLOWED_JOIN_TYPES = {'INNER', 'LEFT', 'RIGHT'}

    def _build_safe_join_query(self, payload):
        """
        Build a multi-table JOIN query from the exploration payload.
        Returns (query: sql.Composed, params: list, aliases: list).
        """
        table_name = payload.get('table_name', '')
        joins = payload.get('joins', [])
        dimensions = payload.get('dimensions', [])
        metrics = payload.get('metrics', [])
        filters = payload.get('filters', [])
        row_limit = payload.get('row_limit')
        if row_limit is not None:
            try:
                row_limit = min(int(row_limit), MAX_ROW_LIMIT)
            except (ValueError, TypeError):
                row_limit = None

        if not table_name:
            raise ValueError('table_name is required.')
        _validate_identifier(table_name, 'table_name')

        if not dimensions and not metrics:
            raise ValueError('At least one dimension or metric is required.')

        # Validate joins
        for j in joins:
            j_table = j.get('table', '')
            j_type = j.get('type', '').upper()
            j_on = j.get('on', {})
            _validate_identifier(j_table, 'join table')
            if j_type not in self.ALLOWED_JOIN_TYPES:
                raise ValueError(
                    f"Join type '{j_type}' is not allowed. "
                    f"Allowed: {', '.join(sorted(self.ALLOWED_JOIN_TYPES))}"
                )
            _validate_identifier(j_on.get('left_column', ''), 'join left column')
            _validate_identifier(j_on.get('right_column', ''), 'join right column')
            left_table = j_on.get('left_table', table_name)
            _validate_identifier(left_table, 'join left table')

        # Helper: resolve table.column notation
        def resolve_column(col_str, default_table):
            """Parse 'table.column' or plain 'column' into (table, column)."""
            if '.' in col_str:
                parts = col_str.split('.', 1)
                _validate_identifier(parts[0], 'table reference')
                _validate_identifier(parts[1], 'column reference')
                return parts[0], parts[1]
            _validate_identifier(col_str, 'column')
            return default_table, col_str

        select_parts = []
        group_by_parts = []
        aliases = []

        # --- dimensions ---
        for dim in dimensions:
            tbl, col = resolve_column(dim, table_name)
            qualified = psql.SQL('{}.{}').format(psql.Identifier(tbl), psql.Identifier(col))
            # Alias as "table_column" to avoid ambiguity
            alias_name = f"{tbl}_{col}" if '.' in dim else col
            select_parts.append(psql.SQL('{} AS {}').format(qualified, psql.Identifier(alias_name)))
            group_by_parts.append(qualified)
            aliases.append(alias_name)

        # --- metrics ---
        for metric in metrics:
            col_str = metric.get('column', '')
            agg = metric.get('aggregate', '').upper()
            alias = metric.get('alias', '')

            tbl, col = resolve_column(col_str, table_name)

            if not alias:
                alias = f"{agg.lower()}_{col}"
            _validate_identifier(alias, 'metric alias')

            if agg == 'COUNT_DISTINCT':
                agg = 'COUNTUNIQUE'
            elif agg == 'AVG':
                agg = 'AVERAGE'

            if agg not in ALLOWED_AGGREGATES:
                raise ValueError(
                    f"Aggregate '{agg}' is not allowed. "
                    f"Allowed: {', '.join(sorted(ALLOWED_AGGREGATES))}"
                )

            qualified_col = psql.SQL('{}.{}').format(psql.Identifier(tbl), psql.Identifier(col))
            alias_ident = psql.Identifier(alias)

            if agg == 'COUNTUNIQUE':
                expr = psql.SQL('COUNT(DISTINCT {}) AS {}').format(qualified_col, alias_ident)
            elif agg == 'COUNTA':
                expr = psql.SQL('COUNT({}) AS {}').format(qualified_col, alias_ident)
            elif agg == 'COUNT':
                expr = psql.SQL("SUM(CASE WHEN {} IS NOT NULL AND {}::text ~ '^-?[0-9]+(\\.[0-9]+)?$' THEN 1 ELSE 0 END) AS {}").format(qualified_col, qualified_col, alias_ident)
            elif agg == 'AVERAGE':
                expr = psql.SQL('AVG({}) AS {}').format(qualified_col, alias_ident)
            elif agg == 'MEDIAN':
                expr = psql.SQL('PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY {}) AS {}').format(qualified_col, alias_ident)
            elif agg == 'PRODUCT':
                expr = psql.SQL('EXP(SUM(LN(NULLIF({}, 0)))) AS {}').format(qualified_col, alias_ident)
            elif agg == 'STDEV':
                expr = psql.SQL('STDDEV_SAMP({}) AS {}').format(qualified_col, alias_ident)
            elif agg == 'STDEVP':
                expr = psql.SQL('STDDEV_POP({}) AS {}').format(qualified_col, alias_ident)
            elif agg == 'VAR':
                expr = psql.SQL('VAR_SAMP({}) AS {}').format(qualified_col, alias_ident)
            elif agg == 'VARP':
                expr = psql.SQL('VAR_POP({}) AS {}').format(qualified_col, alias_ident)
            else:
                agg_sql = psql.SQL(agg)
                expr = psql.SQL('{}({}) AS {}').format(agg_sql, qualified_col, alias_ident)

            select_parts.append(expr)
            aliases.append(alias)

        # --- FROM + JOINs ---
        from_clause = psql.Identifier(table_name)
        for j in joins:
            j_table = j.get('table')
            j_type = j.get('type', 'INNER').upper()
            j_on = j.get('on', {})
            left_table = j_on.get('left_table', table_name)
            left_col = j_on.get('left_column')
            right_col = j_on.get('right_column')

            from_clause = psql.SQL('{} {} JOIN {} ON {}.{} = {}.{}').format(
                from_clause,
                psql.SQL(j_type),
                psql.Identifier(j_table),
                psql.Identifier(left_table),
                psql.Identifier(left_col),
                psql.Identifier(j_table),
                psql.Identifier(right_col),
            )

        # --- Wrap in subquery with optional row limit ---
        subquery = psql.SQL('SELECT * FROM {}').format(from_clause)
        params = []

        # --- WHERE ---
        if filters:
            where_parts = []
            for f in filters:
                f_col_str = f.get('column', '')
                f_op = f.get('operator', '').upper()
                f_val = f.get('value', '')

                tbl, f_col = resolve_column(f_col_str, table_name)
                if f_op not in ALLOWED_OPERATORS:
                    raise ValueError(
                        f"Filter operator '{f_op}' is not allowed. "
                        f"Allowed: {', '.join(sorted(ALLOWED_OPERATORS))}"
                    )

                qualified_col = psql.SQL('{}.{}').format(psql.Identifier(tbl), psql.Identifier(f_col))

                if f_op in ('IS NULL', 'IS NOT NULL'):
                    where_parts.append(
                        psql.SQL('{} {}').format(qualified_col, psql.SQL(f_op))
                    )
                elif f_op in ('IN', 'NOT IN'):
                    values = [v.strip() for v in str(f_val).split(',') if v.strip()]
                    if not values:
                        raise ValueError(f"IN / NOT IN filter for '{f_col_str}' requires at least one value.")
                    placeholders = psql.SQL(', ').join([psql.Placeholder()] * len(values))
                    where_parts.append(
                        psql.SQL('{} {} ({})').format(qualified_col, psql.SQL(f_op), placeholders)
                    )
                    params.extend(values)
                else:
                    where_parts.append(
                        psql.SQL('{} {} {}').format(qualified_col, psql.SQL(f_op), psql.Placeholder())
                    )
                    params.append(f_val)

            subquery = psql.SQL('{} WHERE {}').format(
                subquery,
                psql.SQL(' AND ').join(where_parts),
            )

        if row_limit is not None:
            subquery = psql.SQL('{} LIMIT {}').format(subquery, psql.Literal(row_limit))

        # --- main query ---
        query = psql.SQL('SELECT {fields} FROM ({sub}) AS raw_limited').format(
            fields=psql.SQL(', ').join(select_parts),
            sub=subquery,
        )

        if group_by_parts:
            query = psql.SQL('{} GROUP BY {}').format(
                query,
                psql.SQL(', ').join(group_by_parts),
            )

        return query, params, aliases


class DataAdapterMultiColumnsView(APIView):
    """Retrieve column metadata for multiple tables in a single request (for join builder)."""
    permission_classes = [IsAuthenticated]

    def get(self, request, pk):
        adapter, error = _get_adapter_or_error(pk, request)
        if error:
            return error

        if adapter.adapter_type != 'jdbc':
            return Response(
                {'error': 'Multi-table column discovery is only supported for PostgreSQL (JDBC) adapters.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        tables_param = request.query_params.get('tables', '').strip()
        if not tables_param:
            return Response({'error': 'tables query parameter is required.'}, status=status.HTTP_400_BAD_REQUEST)

        table_names = [t.strip() for t in tables_param.split(',') if t.strip()]
        if len(table_names) > 10:
            return Response({'error': 'Maximum 10 tables allowed per request.'}, status=status.HTTP_400_BAD_REQUEST)

        for t in table_names:
            try:
                _validate_identifier(t, 'table name')
            except ValueError as e:
                return Response({'error': str(e)}, status=status.HTTP_400_BAD_REQUEST)

        conn = None
        try:
            conn = pool_manager.get_connection(adapter)
            result = {}
            with conn.cursor() as cursor:
                for t in table_names:
                    cursor.execute(
                        "SELECT column_name, data_type, is_nullable "
                        "FROM information_schema.columns "
                        "WHERE table_schema = 'public' AND table_name = %s "
                        "ORDER BY ordinal_position",
                        [t]
                    )
                    result[t] = [
                        {
                            'name': row[0],
                            'data_type': row[1],
                            'nullable': row[2] == 'YES',
                        }
                        for row in cursor.fetchall()
                    ]
            return Response({'tables': result})
        except psycopg2.Error as e:
            logger.exception('Failed to list columns for adapter %s, tables %s', pk, table_names)
            return Response(
                {'error': f'Database error: {str(e)}'},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )
        finally:
            if conn:
                pool_manager.release_connection(pk, conn)

