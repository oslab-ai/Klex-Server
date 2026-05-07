"""
Views for accounts app.
"""
from rest_framework import status, generics
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework_simplejwt.tokens import RefreshToken

from .models import User, Organization
from .serializers import (
    UserSerializer,
    UserCreateSerializer,
    AdminSetupSerializer,
    LoginSerializer,
    UserUpdateSerializer,
    OrganizationSerializer,
)


class LoginView(APIView):
    """JWT Login endpoint."""
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = LoginSerializer(data=request.data)
        if not serializer.is_valid():
            # Extract the most useful error message
            errors = serializer.errors
            if 'non_field_errors' in errors:
                # Our custom validation errors come through here
                nfe = errors['non_field_errors']
                if isinstance(nfe, list) and len(nfe) > 0:
                    item = nfe[0]
                    # If it's a dict with 'error' key, extract it
                    if isinstance(item, dict) and 'error' in item:
                        return Response(
                            {'error': item['error']},
                            status=status.HTTP_401_UNAUTHORIZED,
                        )
                    return Response(
                        {'error': str(item)},
                        status=status.HTTP_401_UNAUTHORIZED,
                    )
            return Response(
                {'error': 'Login failed. Please check your credentials.'},
                status=status.HTTP_401_UNAUTHORIZED,
            )

        user = serializer.validated_data['user']

        refresh = RefreshToken.for_user(user)
        return Response({
            'access': str(refresh.access_token),
            'refresh': str(refresh),
            'user': UserSerializer(user).data,
        })


class LogoutView(APIView):
    """Logout endpoint - blacklist refresh token."""
    permission_classes = [IsAuthenticated]

    def post(self, request):
        try:
            refresh_token = request.data.get('refresh')
            if refresh_token:
                token = RefreshToken(refresh_token)
                token.blacklist()
        except Exception:
            pass  # Token might already be blacklisted or invalid
        return Response({'message': 'Logged out successfully'})


class MeView(APIView):
    """Get current user info."""
    permission_classes = [IsAuthenticated]

    def get(self, request):
        return Response(UserSerializer(request.user).data)

    def patch(self, request):
        serializer = UserSerializer(request.user, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response(serializer.data)


class AdminSetupView(APIView):
    """
    Initial admin setup endpoint.
    First user to register becomes admin automatically.
    """
    permission_classes = [AllowAny]

    def get(self, request):
        """Check if admin setup is needed."""
        has_admin = User.objects.filter(is_admin=True).exists()
        has_super_admin = User.objects.filter(is_super_admin=True).exists()
        return Response({'admin_exists': has_admin, 'super_admin_exists': has_super_admin})

    def post(self, request):
        """Create admin user if no admin exists."""
        has_admin = User.objects.filter(is_admin=True).exists()
        if has_admin:
            return Response(
                {'error': 'Admin already exists. Use regular registration.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        serializer = AdminSetupSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = serializer.save()

        refresh = RefreshToken.for_user(user)
        return Response({
            'access': str(refresh.access_token),
            'refresh': str(refresh),
            'user': UserSerializer(user).data,
        }, status=status.HTTP_201_CREATED)


class UserListView(generics.ListCreateAPIView):
    """List all users (admin) or create new user."""
    queryset = User.objects.all().order_by('-created_at')

    def get_serializer_class(self):
        if self.request.method == 'POST':
            return UserCreateSerializer
        return UserSerializer

    def get_permissions(self):
        if self.request.method == 'POST':
            # Admin can create users, first user can self-register
            if not User.objects.exists():
                return [AllowAny()]
        return [IsAuthenticated()]

    def list(self, request, *args, **kwargs):
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )
        return super().list(request, *args, **kwargs)


class UserDetailView(generics.RetrieveUpdateDestroyAPIView):
    """Get, update, or delete a specific user (admin only)."""
    queryset = User.objects.all()
    serializer_class = UserUpdateSerializer

    def get_permissions(self):
        return [IsAuthenticated()]

    def retrieve(self, request, *args, **kwargs):
        if not request.user.is_admin and str(request.user.id) != kwargs.get('pk'):
            return Response(
                {'error': 'Access denied'},
                status=status.HTTP_403_FORBIDDEN
            )
        return super().retrieve(request, *args, **kwargs)

    def update(self, request, *args, **kwargs):
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )
        # Only super admins may change role flags
        if ('is_admin' in request.data or 'is_super_admin' in request.data) and not request.user.is_super_admin:
            return Response(
                {'error': 'Super Admin access required to change role flags'},
                status=status.HTTP_403_FORBIDDEN
            )
        return super().update(request, *args, **kwargs)

    def destroy(self, request, *args, **kwargs):
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )
        user = self.get_object()
        if user.id == request.user.id:
            return Response(
                {'error': 'Cannot delete yourself'},
                status=status.HTTP_400_BAD_REQUEST
            )
        if user.is_super_admin:
            return Response(
                {'error': 'The Super Admin account cannot be deleted'},
                status=status.HTTP_403_FORBIDDEN
            )
        return super().destroy(request, *args, **kwargs)


class UserPermissionsView(APIView):
    """
    Get or update report permissions for a specific user.
    Admin only.
    """
    permission_classes = [IsAuthenticated]

    def get(self, request, pk):
        """Get all report permissions for a user, including org-inherited info."""
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )

        try:
            user = User.objects.get(pk=pk)
        except User.DoesNotExist:
            return Response(
                {'error': 'User not found'},
                status=status.HTTP_404_NOT_FOUND
            )

        from reports.models import (
            Report, ReportPermission, OrganizationReportPermission,
            ReportGroupMember, GroupUserPermission, GroupOrganizationPermission,
        )

        # Get all reports and user's permissions
        reports = Report.objects.all().order_by('report_name')
        user_permissions = {
            p.report_id: p
            for p in ReportPermission.objects.filter(user=user)
        }

        # Get org-level permissions if user belongs to an org
        org_permissions = {}
        if user.organization_id:
            org_permissions = {
                p.report_id: p
                for p in OrganizationReportPermission.objects.filter(
                    organization=user.organization
                )
            }

        # Get group-level permissions
        # 1. Groups the user is directly granted
        user_group_ids = set(
            GroupUserPermission.objects.filter(
                user=user, can_access=True
            ).values_list('group_id', flat=True)
        )
        # 2. Groups the user's org is granted
        if user.organization_id:
            org_group_ids = set(
                GroupOrganizationPermission.objects.filter(
                    organization=user.organization, can_access=True
                ).values_list('group_id', flat=True)
            )
            user_group_ids |= org_group_ids

        # Resolve report IDs accessible via groups
        group_report_ids = set()
        group_schedule_report_ids = set()
        if user_group_ids:
            group_report_ids = set(
                ReportGroupMember.objects.filter(
                    group_id__in=user_group_ids
                ).values_list('report_id', flat=True)
            )
            # Check can_schedule for groups
            schedule_group_ids = set(
                GroupUserPermission.objects.filter(
                    user=user, can_access=True, can_schedule=True
                ).values_list('group_id', flat=True)
            )
            if user.organization_id:
                schedule_group_ids |= set(
                    GroupOrganizationPermission.objects.filter(
                        organization=user.organization,
                        can_access=True,
                        can_schedule=True,
                    ).values_list('group_id', flat=True)
                )
            if schedule_group_ids:
                group_schedule_report_ids = set(
                    ReportGroupMember.objects.filter(
                        group_id__in=schedule_group_ids
                    ).values_list('report_id', flat=True)
                )

        result = []
        for report in reports:
            user_perm = user_permissions.get(report.id)
            org_perm = org_permissions.get(report.id)
            result.append({
                'report_id': report.id,
                'report_name': report.display_name or report.report_name,
                'path': report.path,
                'is_public': report.is_public,
                'can_access': user_perm.can_access if user_perm else False,
                'can_schedule': user_perm.can_schedule if user_perm else False,
                'org_can_access': org_perm.can_access if org_perm else False,
                'org_can_schedule': org_perm.can_schedule if org_perm else False,
                'group_can_access': report.id in group_report_ids,
                'group_can_schedule': report.id in group_schedule_report_ids,
            })

        return Response(result)

    def put(self, request, pk):
        """Bulk update permissions for a user."""
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )

        try:
            user = User.objects.get(pk=pk)
        except User.DoesNotExist:
            return Response(
                {'error': 'User not found'},
                status=status.HTTP_404_NOT_FOUND
            )

        from reports.models import Report, ReportPermission

        permissions_data = request.data.get('permissions', [])

        # Process each permission update
        for perm_data in permissions_data:
            report_id = perm_data.get('report_id')
            can_access = perm_data.get('can_access', False)
            can_schedule = perm_data.get('can_schedule', False)

            try:
                report = Report.objects.get(pk=report_id)
            except Report.DoesNotExist:
                continue

            if can_access:
                # Create or update permission
                ReportPermission.objects.update_or_create(
                    user=user,
                    report=report,
                    defaults={
                        'can_access': can_access,
                        'can_schedule': can_schedule,
                    }
                )
            else:
                # Remove permission if exists
                ReportPermission.objects.filter(
                    user=user,
                    report=report
                ).delete()

        return Response({'message': 'Permissions updated successfully'})


# ---------- Organization Views ----------

class OrganizationListView(generics.ListCreateAPIView):
    """List all organizations or create a new one (admin only)."""
    queryset = Organization.objects.all()
    serializer_class = OrganizationSerializer
    permission_classes = [IsAuthenticated]

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
        return super().create(request, *args, **kwargs)


class OrganizationDetailView(generics.RetrieveUpdateDestroyAPIView):
    """Get, update, or delete a specific organization (admin only)."""
    queryset = Organization.objects.all()
    serializer_class = OrganizationSerializer
    permission_classes = [IsAuthenticated]

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
        return super().update(request, *args, **kwargs)

    def destroy(self, request, *args, **kwargs):
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )
        return super().destroy(request, *args, **kwargs)


class OrganizationMembersView(APIView):
    """Get members of an organization (admin only)."""
    permission_classes = [IsAuthenticated]

    def get(self, request, pk):
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )
        try:
            org = Organization.objects.get(pk=pk)
        except Organization.DoesNotExist:
            return Response(
                {'error': 'Organization not found'},
                status=status.HTTP_404_NOT_FOUND
            )

        members = User.objects.filter(organization=org).order_by('username')
        return Response(UserSerializer(members, many=True).data)


class OrganizationPermissionsView(APIView):
    """
    Get or update report permissions for a specific organization.
    Admin only.
    """
    permission_classes = [IsAuthenticated]

    def get(self, request, pk):
        """Get all report permissions for an organization."""
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )

        try:
            org = Organization.objects.get(pk=pk)
        except Organization.DoesNotExist:
            return Response(
                {'error': 'Organization not found'},
                status=status.HTTP_404_NOT_FOUND
            )

        from reports.models import Report, OrganizationReportPermission

        reports = Report.objects.all().order_by('report_name')
        permissions = {
            p.report_id: p
            for p in OrganizationReportPermission.objects.filter(organization=org)
        }

        result = []
        for report in reports:
            perm = permissions.get(report.id)
            result.append({
                'report_id': report.id,
                'report_name': report.display_name or report.report_name,
                'path': report.path,
                'is_public': report.is_public,
                'can_access': perm.can_access if perm else False,
                'can_schedule': perm.can_schedule if perm else False,
            })

        return Response(result)

    def put(self, request, pk):
        """Bulk update permissions for an organization."""
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )

        try:
            org = Organization.objects.get(pk=pk)
        except Organization.DoesNotExist:
            return Response(
                {'error': 'Organization not found'},
                status=status.HTTP_404_NOT_FOUND
            )

        from reports.models import Report, OrganizationReportPermission

        permissions_data = request.data.get('permissions', [])

        for perm_data in permissions_data:
            report_id = perm_data.get('report_id')
            can_access = perm_data.get('can_access', False)
            can_schedule = perm_data.get('can_schedule', False)

            try:
                report = Report.objects.get(pk=report_id)
            except Report.DoesNotExist:
                continue

            if can_access:
                OrganizationReportPermission.objects.update_or_create(
                    organization=org,
                    report=report,
                    defaults={
                        'can_access': can_access,
                        'can_schedule': can_schedule,
                    }
                )
            else:
                OrganizationReportPermission.objects.filter(
                    organization=org,
                    report=report
                ).delete()

        return Response({'message': 'Organization permissions updated successfully'})


class ReportPublicToggleView(APIView):
    """Toggle a report's public visibility (admin only)."""
    permission_classes = [IsAuthenticated]

    def patch(self, request, pk):
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )

        from reports.models import Report

        try:
            report = Report.objects.get(pk=pk)
        except Report.DoesNotExist:
            return Response(
                {'error': 'Report not found'},
                status=status.HTTP_404_NOT_FOUND
            )

        is_public = request.data.get('is_public')
        if is_public is not None:
            report.is_public = is_public
            report.save(update_fields=['is_public'])

        from reports.serializers import ReportSerializer
        return Response(ReportSerializer(report).data)


# ---------- Report Group Views ----------

class ReportGroupListView(APIView):
    """List all report groups or create a new one (admin only)."""
    permission_classes = [IsAuthenticated]

    def get(self, request):
        """List all report groups with report counts."""
        from reports.models import ReportGroup, ReportGroupMember

        groups = ReportGroup.objects.all().order_by('name')
        result = []
        for group in groups:
            report_ids = list(
                ReportGroupMember.objects.filter(group=group)
                .values_list('report_id', flat=True)
            )
            result.append({
                'id': group.id,
                'name': group.name,
                'color': group.color,
                'description': group.description,
                'report_ids': report_ids,
                'report_count': len(report_ids),
                'created_by': str(group.created_by_id) if group.created_by_id else None,
                'created_at': group.created_at.isoformat(),
                'updated_at': group.updated_at.isoformat(),
            })
        return Response(result)

    def post(self, request):
        """Create a new report group."""
        if not request.user.is_admin:
            return Response(
                {'error': 'Admin access required'},
                status=status.HTTP_403_FORBIDDEN
            )

        from reports.models import ReportGroup, ReportGroupMember, Report

        name = request.data.get('name', '').strip()
        if not name:
            return Response(
                {'error': 'Group name is required'},
                status=status.HTTP_400_BAD_REQUEST
            )

        group = ReportGroup.objects.create(
            name=name,
            color=request.data.get('color', 'violet'),
            description=request.data.get('description', ''),
            created_by=request.user,
        )

        # Optionally add initial reports
        report_ids = request.data.get('report_ids', [])
        if report_ids:
            existing_ids = set(
                Report.objects.filter(id__in=report_ids).values_list('id', flat=True)
            )
            ReportGroupMember.objects.bulk_create([
                ReportGroupMember(group=group, report_id=rid)
                for rid in report_ids if rid in existing_ids
            ])

        report_ids_final = list(
            ReportGroupMember.objects.filter(group=group)
            .values_list('report_id', flat=True)
        )
        return Response({
            'id': group.id,
            'name': group.name,
            'color': group.color,
            'description': group.description,
            'report_ids': report_ids_final,
            'report_count': len(report_ids_final),
            'created_by': str(group.created_by_id) if group.created_by_id else None,
            'created_at': group.created_at.isoformat(),
            'updated_at': group.updated_at.isoformat(),
        }, status=status.HTTP_201_CREATED)


class ReportGroupDetailView(APIView):
    """Get, update, or delete a specific report group (admin only)."""
    permission_classes = [IsAuthenticated]

    def _get_group(self, pk):
        from reports.models import ReportGroup
        try:
            return ReportGroup.objects.get(pk=pk)
        except ReportGroup.DoesNotExist:
            return None

    def _serialize(self, group):
        from reports.models import ReportGroupMember
        report_ids = list(
            ReportGroupMember.objects.filter(group=group)
            .values_list('report_id', flat=True)
        )
        return {
            'id': group.id,
            'name': group.name,
            'color': group.color,
            'description': group.description,
            'report_ids': report_ids,
            'report_count': len(report_ids),
            'created_by': str(group.created_by_id) if group.created_by_id else None,
            'created_at': group.created_at.isoformat(),
            'updated_at': group.updated_at.isoformat(),
        }

    def get(self, request, pk):
        group = self._get_group(pk)
        if not group:
            return Response({'error': 'Group not found'}, status=status.HTTP_404_NOT_FOUND)
        return Response(self._serialize(group))

    def put(self, request, pk):
        if not request.user.is_admin:
            return Response({'error': 'Admin access required'}, status=status.HTTP_403_FORBIDDEN)

        group = self._get_group(pk)
        if not group:
            return Response({'error': 'Group not found'}, status=status.HTTP_404_NOT_FOUND)

        if 'name' in request.data:
            group.name = request.data['name'].strip()
        if 'color' in request.data:
            group.color = request.data['color']
        if 'description' in request.data:
            group.description = request.data['description']
        group.save()

        return Response(self._serialize(group))

    def delete(self, request, pk):
        if not request.user.is_admin:
            return Response({'error': 'Admin access required'}, status=status.HTTP_403_FORBIDDEN)

        group = self._get_group(pk)
        if not group:
            return Response({'error': 'Group not found'}, status=status.HTTP_404_NOT_FOUND)

        group.delete()
        return Response({'message': 'Group deleted'}, status=status.HTTP_204_NO_CONTENT)


class ReportGroupReportsView(APIView):
    """Manage report membership in a group (admin only)."""
    permission_classes = [IsAuthenticated]

    def put(self, request, pk):
        """Set the reports belonging to this group (full replace)."""
        if not request.user.is_admin:
            return Response({'error': 'Admin access required'}, status=status.HTTP_403_FORBIDDEN)

        from reports.models import ReportGroup, ReportGroupMember, Report

        try:
            group = ReportGroup.objects.get(pk=pk)
        except ReportGroup.DoesNotExist:
            return Response({'error': 'Group not found'}, status=status.HTTP_404_NOT_FOUND)

        report_ids = request.data.get('report_ids', [])
        existing_ids = set(
            Report.objects.filter(id__in=report_ids).values_list('id', flat=True)
        )

        # Full replace: remove all, then add
        ReportGroupMember.objects.filter(group=group).delete()
        ReportGroupMember.objects.bulk_create([
            ReportGroupMember(group=group, report_id=rid)
            for rid in report_ids if rid in existing_ids
        ])

        final_ids = list(
            ReportGroupMember.objects.filter(group=group)
            .values_list('report_id', flat=True)
        )
        return Response({
            'group_id': group.id,
            'report_ids': final_ids,
            'report_count': len(final_ids),
        })


class ReportGroupPermissionsView(APIView):
    """
    Get or update user/org permissions for a specific group.
    Admin only.
    """
    permission_classes = [IsAuthenticated]

    def get(self, request, pk):
        """Get all user and org permissions for a group."""
        if not request.user.is_admin:
            return Response({'error': 'Admin access required'}, status=status.HTTP_403_FORBIDDEN)

        from reports.models import ReportGroup, GroupUserPermission, GroupOrganizationPermission

        try:
            group = ReportGroup.objects.get(pk=pk)
        except ReportGroup.DoesNotExist:
            return Response({'error': 'Group not found'}, status=status.HTTP_404_NOT_FOUND)

        users = User.objects.all().order_by('username')
        orgs = Organization.objects.all().order_by('name')

        user_perms = {
            p.user_id: p
            for p in GroupUserPermission.objects.filter(group=group)
        }
        org_perms = {
            p.organization_id: p
            for p in GroupOrganizationPermission.objects.filter(group=group)
        }

        user_result = []
        for u in users:
            perm = user_perms.get(u.id)
            user_result.append({
                'user_id': str(u.id),
                'username': u.username,
                'display_name': u.display_name,
                'is_admin': u.is_admin,
                'can_access': perm.can_access if perm else False,
                'can_schedule': perm.can_schedule if perm else False,
            })

        org_result = []
        for o in orgs:
            perm = org_perms.get(o.id)
            org_result.append({
                'organization_id': o.id,
                'name': o.name,
                'can_access': perm.can_access if perm else False,
                'can_schedule': perm.can_schedule if perm else False,
            })

        return Response({
            'group_id': group.id,
            'group_name': group.name,
            'users': user_result,
            'organizations': org_result,
        })

    def put(self, request, pk):
        """Bulk update user and org permissions for a group."""
        if not request.user.is_admin:
            return Response({'error': 'Admin access required'}, status=status.HTTP_403_FORBIDDEN)

        from reports.models import ReportGroup, GroupUserPermission, GroupOrganizationPermission

        try:
            group = ReportGroup.objects.get(pk=pk)
        except ReportGroup.DoesNotExist:
            return Response({'error': 'Group not found'}, status=status.HTTP_404_NOT_FOUND)

        # Process user permissions
        user_perms = request.data.get('users', [])
        for perm_data in user_perms:
            user_id = perm_data.get('user_id')
            can_access = perm_data.get('can_access', False)
            can_schedule = perm_data.get('can_schedule', False)

            try:
                user = User.objects.get(pk=user_id)
            except User.DoesNotExist:
                continue

            if can_access:
                GroupUserPermission.objects.update_or_create(
                    group=group,
                    user=user,
                    defaults={
                        'can_access': can_access,
                        'can_schedule': can_schedule,
                    }
                )
            else:
                GroupUserPermission.objects.filter(group=group, user=user).delete()

        # Process org permissions
        org_perms = request.data.get('organizations', [])
        for perm_data in org_perms:
            org_id = perm_data.get('organization_id')
            can_access = perm_data.get('can_access', False)
            can_schedule = perm_data.get('can_schedule', False)

            try:
                org = Organization.objects.get(pk=org_id)
            except Organization.DoesNotExist:
                continue

            if can_access:
                GroupOrganizationPermission.objects.update_or_create(
                    group=group,
                    organization=org,
                    defaults={
                        'can_access': can_access,
                        'can_schedule': can_schedule,
                    }
                )
            else:
                GroupOrganizationPermission.objects.filter(
                    group=group, organization=org
                ).delete()

        return Response({'message': 'Group permissions updated successfully'})
