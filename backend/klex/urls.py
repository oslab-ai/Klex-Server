"""
URL configuration for klex project.
"""
from django.contrib import admin
from django.urls import path, include
from django.conf import settings
from django.conf.urls.static import static

urlpatterns = [
    path('admin/', admin.site.urls),
    path('api/auth/', include('accounts.urls')),
    path('api/repos/', include('repos.urls')),
    path('api/reports/', include('reports.urls')),
    path('api/github/', include('github_integration.urls')),
    path('api/data-adapters/', include('dataadapter.urls')),
    path('api/audit-logs/', include('audit.urls')),
]

# Serve media files in development
if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
