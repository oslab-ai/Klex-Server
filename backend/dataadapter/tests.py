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
