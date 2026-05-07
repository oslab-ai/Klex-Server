import os
import sys
import logging
import time
from datetime import datetime

from airflow import DAG
from airflow.providers.standard.operators.python import PythonOperator
from airflow.models import DagRun

# Ensure the Django project directory is in the Python path
BACKEND_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), '../../'))
if BACKEND_DIR not in sys.path:
    sys.path.insert(0, BACKEND_DIR)

# Initialize Django
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'klex.settings')
import django
django.setup()

from django.conf import settings
from django.core.mail import EmailMessage
import requests as http_requests

logger = logging.getLogger(__name__)

default_args = {
    'owner': 'klex',
    'depends_on_past': False,
    'start_date': datetime(2023, 1, 1),
    'email_on_failure': False,
    'email_on_retry': False,
    'retries': 0,
}

def _compile_report(**kwargs):
    dag_run: DagRun = kwargs['dag_run']
    conf = dag_run.conf or {}
    
    export_formats = conf.get('output_formats', {}).get('outputFormat', ['PDF'])
    if isinstance(export_formats, str):
        export_formats = [export_formats]
        
    local_jrxml_path = conf.get('report_unit_uri')
    data_adapter = conf.get('data_adapter', {})
    parameters = conf.get('parameters', {})
    
    if not local_jrxml_path:
        raise ValueError("No report_unit_uri provided in DAG conf.")

    FORMAT_EXTENSIONS = {
        'PDF': 'pdf', 'XLSX': 'xlsx', 'CSV': 'csv', 'HTML': 'html',
        'DOCX': 'docx', 'PPTX': 'pptx', 'ODT': 'odt', 'XLS': 'xls'
    }
    
    # Sanitize run_id to be safe for filenames
    safe_run_id = "".join(c for c in dag_run.run_id if c.isalnum() or c in ('-', '_')).rstrip()
    output_dir = os.path.join(settings.MEDIA_ROOT, 'reports', f"scheduled_{safe_run_id}")
    os.makedirs(output_dir, exist_ok=True)
    
    compiled_report_paths = []
    
    for export_format in export_formats:
        file_ext = FORMAT_EXTENSIONS.get(export_format.upper(), 'pdf')
        output_filename = f"report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.{file_ext}"
        java_output_path = os.path.join(output_dir, output_filename)

        compiler_payload = {
            'sourceType': 'LOCAL',
            'path': str(local_jrxml_path),
            'format': export_format.upper(),
            'outputPath': java_output_path,
        }

        if parameters:
            compiler_payload['parameters'] = parameters

        if data_adapter:
            compiler_payload.update(data_adapter)
            
        compiler_url = f"{settings.COMPILER_SERVICE_URL}/api/reports/generate"
        logger.info(f"Calling compiler service at {compiler_url} for format {export_format}")
        
        response = http_requests.post(compiler_url, json=compiler_payload, timeout=120)
        if response.status_code != 200:
            raise Exception(f"Compiler service returned {response.status_code}: {response.text}")
            
        content_type = response.headers.get('Content-Type', '')
        if 'application/' in content_type and 'json' not in content_type:
            file_bytes = response.content
            with open(java_output_path, 'wb') as f:
                f.write(file_bytes)
        else:
            result_text = response.text.strip()
            if result_text.startswith('Error'):
                raise Exception(result_text)
            if not os.path.exists(java_output_path):
                raise Exception(f"Java service success but output not found at {java_output_path}")

        compiled_report_paths.append(java_output_path)

    # Pass the generated file paths to the next task
    kwargs['ti'].xcom_push(key='compiled_report_paths', value=compiled_report_paths)

def _send_email(**kwargs):
    dag_run: DagRun = kwargs['dag_run']
    conf = dag_run.conf or {}
    
    delivery_method = conf.get('delivery_method', 'EMAIL')
    if delivery_method != 'EMAIL':
        logger.info(f"Delivery method is {delivery_method}, skipping email.")
        return
        
    mail_notification = conf.get('mail_notification')
    if not mail_notification:
        logger.warning("No mail_notification config provided, skipping email.")
        return
        
    to_addresses = mail_notification.get('toAddresses', {}).get('address', [])
    if isinstance(to_addresses, str):
        to_addresses = [to_addresses]

    # Dynamically expand to all active organization members with emails
    send_to_organization = mail_notification.get('sendToOrganization') == True
    send_to_organizations = mail_notification.get('sendToOrganizations', [])
    org_id = conf.get('_organization_id')
    
    org_ids_to_fetch = set(send_to_organizations)
    if send_to_organization and org_id:
        org_ids_to_fetch.add(org_id)
        
    if org_ids_to_fetch:
        try:
            from accounts.models import User
            org_users = User.objects.filter(
                organization_id__in=list(org_ids_to_fetch), 
                is_active=True
            ).exclude(email__isnull=True).exclude(email='')
            
            for u in org_users:
                if u.email not in to_addresses:
                    to_addresses.append(u.email)
            logger.info(f"Expanded TO addresses with organization members. Total recipients: {len(to_addresses)}")
        except Exception as e:
            logger.error(f"Failed to expand organization emails: {e}")
    
    if not to_addresses:
        logger.warning("No recipient addresses provided, skipping email.")
        return
        
    subject = mail_notification.get('subject', 'Scheduled Report')
    message_text = mail_notification.get('messageText', 'Please find your scheduled report attached.')
    
    # Retrieve compiled report paths from XCom
    compiled_report_paths = kwargs['ti'].xcom_pull(task_ids='compile_report', key='compiled_report_paths') or []
    
    if not compiled_report_paths:
        raise Exception("No compiled report files were missing/returned from the compile step")

    logger.info(f"Sending email to {to_addresses} with {len(compiled_report_paths)} attachments")
    
    email = EmailMessage(
        subject=subject,
        body=message_text,
        from_email=settings.DEFAULT_FROM_EMAIL,
        to=to_addresses,
    )
    
    schedule_name = conf.get('schedule_name', 'report')
    safe_name = "".join(c for c in schedule_name if c.isalnum() or c in (' ', '-', '_')).strip()
    
    for path in compiled_report_paths:
        if os.path.exists(path):
            file_ext = os.path.splitext(path)[1]
            if not file_ext:
                file_ext = '.pdf'
            filename = f"{safe_name}{file_ext}"
            
            with open(path, 'rb') as f:
                email.attach(filename, f.read())
        else:
            logger.warning(f"File {path} does not exist to attach")

    email.send(fail_silently=False)
    logger.info("Email sent successfully.")

def _update_status(**kwargs):
    from reports.dispatch_service import ReportDispatchService

    dag_run = kwargs['dag_run']
    conf = dag_run.conf or {}

    remaining = conf.get("remaining_occurrences", 1)

    logger.info(f"Run completed. Remaining occurrences: {remaining}")

    if remaining > 1:
        service = ReportDispatchService()

        new_conf = {
            **conf,
            "remaining_occurrences": remaining - 1
        }
        time.sleep(2) 

        service.submit_report_job(
            dag_id="klex_report_dispatcher",
            report_id=conf.get("report_id"),
            payload=new_conf
        )

        logger.info(f"Triggered next occurrence. Remaining: {remaining - 1}")
    else:
        logger.info("All occurrences completed.")

with DAG(
    'klex_report_dispatcher',
    default_args=default_args,
    description='Universal DAG to compile and dispatch Klex reports',
    schedule=None,  # Triggered externally only
    catchup=False,
    is_paused_upon_creation=False,  # <--- CRITICAL: Prevents Airflow from leaving new instances paused
    tags=['klex'],
) as dag:

    compile_task = PythonOperator(
        task_id='compile_report',
        python_callable=_compile_report,
    )

    email_task = PythonOperator(
        task_id='send_email',
        python_callable=_send_email,
    )
    
    status_task = PythonOperator(
        task_id='update_status',
        python_callable=_update_status,
    )

    compile_task >> email_task >> status_task
