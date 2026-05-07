"""
URL patterns for accounts app.
"""
from django.urls import path
from rest_framework_simplejwt.views import TokenRefreshView

from .views import (
    LoginView,
    LogoutView,
    MeView,
    AdminSetupView,
    UserListView,
    UserDetailView,
    UserPermissionsView,
    OrganizationListView,
    OrganizationDetailView,
    OrganizationMembersView,
    OrganizationPermissionsView,
    ReportPublicToggleView,
    ReportGroupListView,
    ReportGroupDetailView,
    ReportGroupReportsView,
    ReportGroupPermissionsView,
)

urlpatterns = [
    path('login/', LoginView.as_view(), name='login'),
    path('logout/', LogoutView.as_view(), name='logout'),
    path('refresh/', TokenRefreshView.as_view(), name='token_refresh'),
    path('me/', MeView.as_view(), name='me'),
    path('admin-setup/', AdminSetupView.as_view(), name='admin-setup'),
    path('users/', UserListView.as_view(), name='user-list'),
    path('users/<uuid:pk>/', UserDetailView.as_view(), name='user-detail'),
    path('users/<uuid:pk>/permissions/', UserPermissionsView.as_view(), name='user-permissions'),
    # Organizations
    path('organizations/', OrganizationListView.as_view(), name='organization-list'),
    path('organizations/<int:pk>/', OrganizationDetailView.as_view(), name='organization-detail'),
    path('organizations/<int:pk>/members/', OrganizationMembersView.as_view(), name='organization-members'),
    path('organizations/<int:pk>/permissions/', OrganizationPermissionsView.as_view(), name='organization-permissions'),
    # Report public toggle
    path('reports/<int:pk>/toggle-public/', ReportPublicToggleView.as_view(), name='report-toggle-public'),
    # Report Groups
    path('groups/', ReportGroupListView.as_view(), name='group-list'),
    path('groups/<int:pk>/', ReportGroupDetailView.as_view(), name='group-detail'),
    path('groups/<int:pk>/reports/', ReportGroupReportsView.as_view(), name='group-reports'),
    path('groups/<int:pk>/permissions/', ReportGroupPermissionsView.as_view(), name='group-permissions'),
]
