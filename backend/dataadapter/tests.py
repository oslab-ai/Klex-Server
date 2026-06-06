from django.test import TestCase
from rest_framework.test import APIClient
from rest_framework import status

from accounts.models import User
from dataadapter.models import DataAdapter


class DataAdapterModelTests(TestCase):
    """Tests for the DataAdapter model."""

    def setUp(self):
        self.user = User.objects.create_user(
            username='testuser',
            password='testpass123'
        )

    def test_create_jdbc_adapter(self):
        """Test creating a JDBC adapter."""
        adapter = DataAdapter.objects.create(
            name='Test PostgreSQL',
            adapter_type='jdbc',
            connection_details={
                'host': 'localhost',
                'port': 5432,
                'database': 'testdb',
                'username': 'postgres',
                'password': 'secret'
            },
            created_by=self.user
        )
        self.assertEqual(adapter.name, 'Test PostgreSQL')
        self.assertEqual(adapter.adapter_type, 'jdbc')
        self.assertTrue(adapter.is_active)

    def test_create_mock_adapter(self):
        """Test creating a mock adapter."""
        adapter = DataAdapter.objects.create(
            name='Mock Data',
            adapter_type='mock',
            connection_details={},
            created_by=self.user
        )
        self.assertEqual(adapter.adapter_type, 'mock')


class DataAdapterAPITests(TestCase):
    """Tests for data adapter API endpoints."""

    def setUp(self):
        self.client = APIClient()
        self.admin = User.objects.create_superuser(
            username='admin',
            password='adminpass123'
        )
        self.regular_user = User.objects.create_user(
            username='regular',
            password='userpass123'
        )

    def test_list_adapters_admin(self):
        """Test admin can list adapters."""
        self.client.force_authenticate(user=self.admin)
        response = self.client.get('/api/data-adapters/')
        self.assertEqual(response.status_code, status.HTTP_200_OK)

    def test_list_adapters_non_admin(self):
        """Test non-admin cannot list adapters."""
        self.client.force_authenticate(user=self.regular_user)
        response = self.client.get('/api/data-adapters/')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_create_adapter_admin(self):
        """Test admin can create adapter."""
        self.client.force_authenticate(user=self.admin)
        response = self.client.post('/api/data-adapters/', {
            'name': 'Test Adapter',
            'adapter_type': 'mock',
            'connection_details': {}
        }, format='json')
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

    def test_test_connection_mock(self):
        """Test testing mock adapter connection."""
        self.client.force_authenticate(user=self.admin)
        response = self.client.post('/api/data-adapters/test/', {
            'adapter_type': 'mock',
            'connection_details': {}
        }, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(response.data['success'])

    def test_test_connection_jdbc_missing_fields(self):
        """Test JDBC adapter validation requires all fields."""
        self.client.force_authenticate(user=self.admin)
        response = self.client.post('/api/data-adapters/', {
            'name': 'Bad Adapter',
            'adapter_type': 'jdbc',
            'connection_details': {
                'host': 'localhost'
                # Missing: port, database, username, password
            }
        }, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)


class DataAdapterQueryTests(TestCase):
    """Tests for the dynamic data exploration query and schema discovery endpoints."""

    def setUp(self):
        self.client = APIClient()
        self.admin = User.objects.create_superuser(
            username='admin',
            password='adminpass123'
        )
        self.regular_user = User.objects.create_user(
            username='regular',
            password='userpass123'
        )
        self.jdbc_adapter = DataAdapter.objects.create(
            name='Test Postgres',
            adapter_type='jdbc',
            connection_details={
                'host': 'localhost',
                'port': 5432,
                'database': 'testdb',
                'username': 'postgres',
                'password': 'secret'
            },
            created_by=self.admin
        )
        self.mock_adapter = DataAdapter.objects.create(
            name='Mock Adapter',
            adapter_type='mock',
            connection_details={},
            created_by=self.admin
        )

    def test_tables_endpoint_non_admin(self):
        """Test non-admin is forbidden from listing tables."""
        self.client.force_authenticate(user=self.regular_user)
        response = self.client.get(f'/api/data-adapters/{self.jdbc_adapter.id}/tables/')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_tables_endpoint_non_jdbc(self):
        """Test listing tables on non-jdbc adapter fails."""
        self.client.force_authenticate(user=self.admin)
        response = self.client.get(f'/api/data-adapters/{self.mock_adapter.id}/tables/')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('only supported for PostgreSQL', response.data['error'])

    def test_columns_endpoint_invalid_table_identifier(self):
        """Test that invalid table names are rejected before executing query."""
        self.client.force_authenticate(user=self.admin)
        response = self.client.get(
            f'/api/data-adapters/{self.jdbc_adapter.id}/columns/',
            {'table': 'users; DROP TABLE users;--'}
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('Invalid table name', response.data['error'])

    def test_query_endpoint_sql_builder_validation(self):
        """Test validation rules of the secure SQL builder."""
        self.client.force_authenticate(user=self.admin)

        # 1. Invalid table name
        payload = {
            'table_name': 'users; drop table users',
            'dimensions': ['name'],
            'metrics': [{'column': 'salary', 'aggregate': 'SUM'}],
        }
        response = self.client.post(
            f'/api/data-adapters/{self.jdbc_adapter.id}/query/',
            payload,
            format='json'
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('Invalid table_name', response.data['error'])

        # 2. Invalid dimension name
        payload = {
            'table_name': 'users',
            'dimensions': ['name; select 1'],
            'metrics': [{'column': 'salary', 'aggregate': 'SUM'}],
        }
        response = self.client.post(
            f'/api/data-adapters/{self.jdbc_adapter.id}/query/',
            payload,
            format='json'
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('Invalid dimension', response.data['error'])

        # 3. Disallowed aggregate function
        payload = {
            'table_name': 'users',
            'dimensions': ['name'],
            'metrics': [{'column': 'salary', 'aggregate': 'DELETE'}],
        }
        response = self.client.post(
            f'/api/data-adapters/{self.jdbc_adapter.id}/query/',
            payload,
            format='json'
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('Aggregate \'DELETE\' is not allowed', response.data['error'])

        # 4. Disallowed filter operator
        payload = {
            'table_name': 'users',
            'dimensions': ['name'],
            'metrics': [{'column': 'salary', 'aggregate': 'SUM'}],
            'filters': [{'column': 'age', 'operator': 'XOR', 'value': '10'}],
        }
        response = self.client.post(
            f'/api/data-adapters/{self.jdbc_adapter.id}/query/',
            payload,
            format='json'
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('Filter operator \'XOR\' is not allowed', response.data['error'])

    def test_query_endpoint_allows_spaces_and_hyphens(self):
        """Test that safe SQL identifiers with spaces and hyphens pass validation."""
        self.client.force_authenticate(user=self.admin)
        payload = {
            'table_name': 'users-table',
            'dimensions': ['Customer Name'],
            'metrics': [{'column': 'total-amount', 'aggregate': 'SUM'}],
        }
        response = self.client.post(
            f'/api/data-adapters/{self.jdbc_adapter.id}/query/',
            payload,
            format='json'
        )
        # It should pass validation and try to connect to DB, resulting in a database connection error (500) rather than a validation error (400)
        self.assertNotEqual(response.status_code, status.HTTP_400_BAD_REQUEST)


class DataAdapterCSVQueryTests(TestCase):
    """Tests for CSV adapter dynamic data exploration query and schema discovery."""

    def setUp(self):
        import os
        from django.conf import settings
        self.client = APIClient()
        self.admin = User.objects.create_superuser(
            username='admin',
            password='adminpass123'
        )
        self.regular_user = User.objects.create_user(
            username='regular',
            password='userpass123'
        )

        # Create a test CSV file
        os.makedirs(settings.MEDIA_ROOT, exist_ok=True)
        self.csv_file_path = os.path.join(settings.MEDIA_ROOT, 'test_sales.csv')
        with open(self.csv_file_path, 'w', encoding='utf-8') as f:
            f.write("Region,Category,Amount,Quantity\n")
            f.write("East,Office,150.0,3\n")
            f.write("East,Furniture,450.0,2\n")
            f.write("West,Office,120.0,4\n")
            f.write("West,Furniture,300.0,1\n")
            f.write("West,Office,80.0,2\n")

        self.csv_adapter = DataAdapter.objects.create(
            name='Test CSV',
            adapter_type='csv',
            connection_details={
                'file_path': self.csv_file_path,
                'original_name': 'test_sales.csv',
                'file_size': 148
            },
            created_by=self.admin
        )

    def tearDown(self):
        import os
        if os.path.exists(self.csv_file_path):
            os.remove(self.csv_file_path)

    def test_csv_tables_endpoint(self):
        """Test listing tables for a CSV adapter returns the file name."""
        self.client.force_authenticate(user=self.admin)
        response = self.client.get(f'/api/data-adapters/{self.csv_adapter.id}/tables/')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['tables'], ['test_sales'])

    def test_csv_columns_endpoint(self):
        """Test retrieving columns and types for a CSV adapter."""
        self.client.force_authenticate(user=self.admin)
        response = self.client.get(
            f'/api/data-adapters/{self.csv_adapter.id}/columns/',
            {'table': 'test_sales'}
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        cols = response.data['columns']
        self.assertEqual(len(cols), 4)
        
        # Verify columns and inferred types
        region_col = next(c for c in cols if c['name'] == 'Region')
        amount_col = next(c for c in cols if c['name'] == 'Amount')
        quantity_col = next(c for c in cols if c['name'] == 'Quantity')
        
        self.assertEqual(region_col['data_type'], 'varchar')
        self.assertEqual(amount_col['data_type'], 'numeric')
        self.assertEqual(quantity_col['data_type'], 'numeric')

    def test_csv_query_endpoint_aggregated(self):
        """Test aggregation query on CSV without dimensions."""
        self.client.force_authenticate(user=self.admin)
        payload = {
            'table_name': 'test_sales',
            'dimensions': [],
            'metrics': [
                {'column': 'Amount', 'aggregate': 'SUM', 'alias': 'total_amount'},
                {'column': 'Quantity', 'aggregate': 'AVG', 'alias': 'avg_qty'},
                {'column': 'Region', 'aggregate': 'COUNTA', 'alias': 'row_count'}
            ],
            'filters': []
        }
        response = self.client.post(
            f'/api/data-adapters/{self.csv_adapter.id}/query/',
            payload,
            format='json'
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        data = response.data['data']
        self.assertEqual(len(data), 1)
        # 150 + 450 + 120 + 300 + 80 = 1100
        self.assertEqual(float(data[0]['total_amount']), 1100.0)
        # (3 + 2 + 4 + 1 + 2) / 5 = 2.4
        self.assertEqual(float(data[0]['avg_qty']), 2.4)
        self.assertEqual(data[0]['row_count'], 5)

    def test_csv_query_endpoint_group_by(self):
        """Test grouping and filters on CSV data query."""
        self.client.force_authenticate(user=self.admin)
        payload = {
            'table_name': 'test_sales',
            'dimensions': ['Region'],
            'metrics': [
                {'column': 'Amount', 'aggregate': 'SUM', 'alias': 'total_amount'}
            ],
            'filters': [
                {'column': 'Category', 'operator': '=', 'value': 'Office'}
            ]
        }
        response = self.client.post(
            f'/api/data-adapters/{self.csv_adapter.id}/query/',
            payload,
            format='json'
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        data = response.data['data']
        # Group by Region should have East and West
        self.assertEqual(len(data), 2)
        
        east_row = next(r for r in data if r['Region'] == 'East')
        west_row = next(r for r in data if r['Region'] == 'West')
        
        # East Office amount: 150.0
        self.assertEqual(float(east_row['total_amount']), 150.0)
        # West Office amount: 120.0 + 80.0 = 200.0
        self.assertEqual(float(west_row['total_amount']), 200.0)

    def test_csv_distinct_values(self):
        """Test retrieving distinct values of a CSV column."""
        self.client.force_authenticate(user=self.admin)
        response = self.client.get(
            f'/api/data-adapters/{self.csv_adapter.id}/distinct-values/',
            {'table': 'test_sales', 'column': 'Region'}
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['values'], ['East', 'West'])

    def test_csv_median_product_query(self):
        """Test MEDIAN and PRODUCT aggregate queries on CSV data adapter."""
        self.client.force_authenticate(user=self.admin)
        payload = {
            'table_name': 'test_sales',
            'dimensions': ['Region'],
            'metrics': [
                {'column': 'Amount', 'aggregate': 'MEDIAN', 'alias': 'median_amount'},
                {'column': 'Quantity', 'aggregate': 'PRODUCT', 'alias': 'product_qty'}
            ],
            'filters': []
        }
        response = self.client.post(
            f'/api/data-adapters/{self.csv_adapter.id}/query/',
            payload,
            format='json'
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        data = response.data['data']
        self.assertEqual(len(data), 2)

        east_row = next(r for r in data if r['Region'] == 'East')
        west_row = next(r for r in data if r['Region'] == 'West')

        # East: Amount values are 150.0, 450.0. Median of two values is average: (150+450)/2 = 300.0
        self.assertEqual(float(east_row['median_amount']), 300.0)
        # East: Quantities are 3, 2. Product is 6.0
        self.assertEqual(float(east_row['product_qty']), 6.0)

        # West: Amount values are 120.0, 300.0, 80.0. Sorted: 80.0, 120.0, 300.0. Median is 120.0
        self.assertEqual(float(west_row['median_amount']), 120.0)
        # West: Quantities are 4, 1, 2. Product is 8.0
        self.assertEqual(float(west_row['product_qty']), 8.0)




