from django.urls import path
from .views import ChatbotAPIView

urlpatterns = [
    path('', ChatbotAPIView.as_view(), name='chatbot_api'),
]
