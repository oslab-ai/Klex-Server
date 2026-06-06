from django.apps import AppConfig

class DataadapterConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'dataadapter'

    _initialized = False

    def ready(self):
        if not DataadapterConfig._initialized:
            DataadapterConfig._initialized = True
            import os
            import logging
            logger = logging.getLogger(__name__)
            try:
                from phoenix.otel import register
                project_name = os.getenv('PHOENIX_PROJECT_NAME', 'klex-data-exploration')
                endpoint = os.getenv('PHOENIX_COLLECTOR_ENDPOINT', 'http://localhost:4317')
                register(
                    project_name=project_name,
                    endpoint=endpoint
                )
                logger.info(f"Arize Phoenix OTel registered successfully for project '{project_name}' targeting endpoint '{endpoint}'")
            except Exception as e:
                logger.warning(f"Could not register Arize Phoenix OTel: {e}")
