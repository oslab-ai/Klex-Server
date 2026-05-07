"""
Serializers for accounts app.
"""
from rest_framework import serializers
from django.contrib.auth import authenticate
from .models import User, Organization


class OrganizationSerializer(serializers.ModelSerializer):
    """Serializer for Organization model."""
    member_count = serializers.SerializerMethodField()

    class Meta:
        model = Organization
        fields = ['id', 'name', 'description', 'member_count', 'created_at']
        read_only_fields = ['id', 'created_at']

    def get_member_count(self, obj):
        return obj.members.count()


class OrganizationMinimalSerializer(serializers.ModelSerializer):
    """Minimal serializer for embedding org info in user responses."""

    class Meta:
        model = Organization
        fields = ['id', 'name']


class UserSerializer(serializers.ModelSerializer):
    """Serializer for User model."""
    organization_name = serializers.CharField(source='organization.name', read_only=True, default=None)

    class Meta:
        model = User
        fields = ['id', 'username', 'email', 'display_name', 'is_admin', 'is_super_admin', 'is_active', 'organization', 'organization_name', 'created_at']
        read_only_fields = ['id', 'created_at']


class UserCreateSerializer(serializers.ModelSerializer):
    """Serializer for creating new users."""
    password = serializers.CharField(write_only=True, min_length=8)

    class Meta:
        model = User
        fields = ['username', 'email', 'password', 'display_name', 'organization']

    def validate_email(self, value):
        if value and User.objects.filter(email=value).exists():
            raise serializers.ValidationError('Email already registered')
        return value

    def create(self, validated_data):
        return User.objects.create_user(**validated_data)


class AdminSetupSerializer(serializers.Serializer):
    """Serializer for initial admin setup."""
    username = serializers.CharField(max_length=150)
    email = serializers.EmailField(required=False)
    password = serializers.CharField(write_only=True, min_length=8)
    display_name = serializers.CharField(max_length=255, required=False)

    def validate_username(self, value):
        if User.objects.filter(username=value).exists():
            raise serializers.ValidationError('Username already exists')
        return value

    def validate_email(self, value):
        if value and User.objects.filter(email=value).exists():
            raise serializers.ValidationError('Email already registered')
        return value

    def create(self, validated_data):
        # First user becomes super admin and admin automatically
        is_first_user = not User.objects.exists()
        user = User.objects.create_user(
            username=validated_data['username'],
            password=validated_data['password'],
            email=validated_data.get('email'),
            display_name=validated_data.get('display_name'),
            is_admin=is_first_user,
            is_super_admin=is_first_user,
            is_staff=is_first_user,
        )
        return user


class LoginSerializer(serializers.Serializer):
    """Serializer for login. Supports login via username or email."""
    username = serializers.CharField()
    password = serializers.CharField(write_only=True)

    def validate(self, data):
        username_or_email = data['username']
        password = data['password']

        # If input looks like an email, resolve it to a username
        lookup_username = username_or_email
        if '@' in username_or_email:
            try:
                user_obj = User.objects.get(email=username_or_email)
                lookup_username = user_obj.username
            except User.DoesNotExist:
                raise serializers.ValidationError(
                    'No account found with this email address'
                )
        else:
            if not User.objects.filter(username=username_or_email).exists():
                raise serializers.ValidationError(
                    'No account found with this username'
                )

        user = authenticate(username=lookup_username, password=password)
        if not user:
            # User exists but password is wrong
            raise serializers.ValidationError(
                'Incorrect password'
            )
        if not user.is_active:
            raise serializers.ValidationError(
                'This account has been disabled. Contact your administrator.'
            )
        data['user'] = user
        return data


class UserUpdateSerializer(serializers.ModelSerializer):
    """Serializer for updating users (admin only)."""
    password = serializers.CharField(
        write_only=True, min_length=8, required=False, allow_blank=True,
    )

    class Meta:
        model = User
        fields = [
            'username', 'email', 'display_name', 'password',
            'is_active', 'is_admin', 'is_super_admin', 'organization',
        ]

    def update(self, instance, validated_data):
        password = validated_data.pop('password', None)
        instance = super().update(instance, validated_data)
        if password:
            instance.set_password(password)
            instance.save(update_fields=['password'])
        return instance
