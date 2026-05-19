import os
import sys
import logging
from datetime import datetime

from airflow import DAG
from airflow.providers.standard.operators.python import PythonOperator
from airflow.models import DagRun

# Ensure the Django project directory is in the Python path
BACKEND_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), '../../'))
if BACKEND_DIR not in sys.path:
    sys.path.insert(0, BACKEND_DIR)

# Initialize Django
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'jaspit.settings')
import django
django.setup()

from django.conf import settings
from django.core.mail import EmailMessage
import requests as http_requests

logger = logging.getLogger(__name__)


def _on_task_failure(context):
    """Callback to immediately update ReportExecutionJob on task failure."""
    from reports.models import ReportExecutionJob
    from reports.enums import JobStatus

    dag_run = context.get('dag_run')
    if not dag_run:
        return

    run_id = dag_run.run_id
    try:
        job = ReportExecutionJob.objects.get(airflow_run_id=run_id)
        if job.status not in JobStatus.terminal_statuses():
            job.status = JobStatus.FAILED_RETRYABLE
            job.last_error = str(context.get('exception', 'Unknown error'))[:500]
            job.save(update_fields=['status', 'last_error', 'updated_at'])
            logger.info(f"Job {job.id} marked FAILED_RETRYABLE for run {run_id}")
    except ReportExecutionJob.DoesNotExist:
        logger.warning(f"No ReportExecutionJob found for run_id={run_id}")
    except Exception as e:
        logger.error(f"Failed to update job status on failure for run {run_id}: {e}")


default_args = {
    'owner': 'jasper',
    'depends_on_past': False,
    'start_date': datetime(2023, 1, 1),
    'email_on_failure': False,
    'email_on_retry': False,
    'retries': 0,
    'on_failure_callback': _on_task_failure,
}

def _compile_report(**kwargs):
    dag_run: DagRun = kwargs['dag_run']
    conf = dag_run.conf or {}
    
    export_format = conf.get('output_formats', {}).get('outputFormat', ['PDF'])[0]
    local_jrxml_path = conf.get('report_unit_uri')
    data_adapter = conf.get('data_adapter', {})
    parameters = conf.get('parameters', {})
    
    if not local_jrxml_path:
        raise ValueError("No report_unit_uri provided in DAG conf.")

    FORMAT_EXTENSIONS = {
        'PDF': 'pdf', 'XLSX': 'xlsx', 'CSV': 'csv', 'HTML': 'html',
        'DOCX': 'docx', 'PPTX': 'pptx', 'ODT': 'odt', 'XLS': 'xls'
    }
    file_ext = FORMAT_EXTENSIONS.get(export_format.upper(), 'pdf')
    
    # Sanitize run_id to be safe for filenames
    safe_run_id = "".join(c for c in dag_run.run_id if c.isalnum() or c in ('-', '_')).rstrip()
    
    output_dir = os.path.join(settings.MEDIA_ROOT, 'reports', f"scheduled_{safe_run_id}")
    os.makedirs(output_dir, exist_ok=True)
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
    logger.info(f"Calling compiler service at {compiler_url}")
    
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

    # Pass the generated file path to the next task
    kwargs['ti'].xcom_push(key='compiled_report_path', value=java_output_path)
    kwargs['ti'].xcom_push(key='file_ext', value=file_ext)

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
    
    if not to_addresses:
        logger.warning("No recipient addresses provided, skipping email.")
        return
        
    subject = mail_notification.get('subject', 'Scheduled Report')
    message_text = mail_notification.get('messageText', 'Please find your scheduled report attached.')
    
    # Retrieve compiled report path from XCom
    compiled_report_path = kwargs['ti'].xcom_pull(task_ids='compile_report', key='compiled_report_path')
    file_ext = kwargs['ti'].xcom_pull(task_ids='compile_report', key='file_ext') or 'pdf'
    
    if not compiled_report_path or not os.path.exists(compiled_report_path):
        raise Exception(f"Compiled report file missing at {compiled_report_path}")

    # Clean schedule name for filename
    schedule_name = conf.get('schedule_name', 'report')
    safe_name = "".join(c for c in schedule_name if c.isalnum() or c in (' ', '-', '_')).strip()
    filename = f"{safe_name}.{file_ext}"
    
    logger.info(f"Sending email to {to_addresses} with attachment {filename}")
    
    email = EmailMessage(
        subject=subject,
        body=message_text,
        from_email=settings.DEFAULT_FROM_EMAIL,
        to=to_addresses,
    )
    email.attach_file(compiled_report_path)
    email.send(fail_silently=False)
    logger.info("Email sent successfully.")

def _update_status(**kwargs):
    """
    Update the ReportExecutionJob status in Django after successful execution.
    This provides immediate feedback rather than waiting for reconcile_airflow.
    """
    from reports.models import ReportExecutionJob
    from reports.enums import JobStatus

    dag_run: DagRun = kwargs['dag_run']
    conf = dag_run.conf or {}

    # Find the job by its airflow_run_id
    run_id = dag_run.run_id
    try:
        job = ReportExecutionJob.objects.get(airflow_run_id=run_id)
        job.status = JobStatus.SUCCESS
        job.completed_at = datetime.now()
        job.save(update_fields=['status', 'completed_at', 'updated_at'])
        logger.info(f"Job {job.id} marked SUCCESS for run {run_id}")
    except ReportExecutionJob.DoesNotExist:
        logger.warning(f"No ReportExecutionJob found for run_id={run_id}")
    except Exception as e:
        # Don't fail the DAG if status update fails — reconciliation will catch it
        logger.error(f"Failed to update job status for run {run_id}: {e}")

with DAG(
    'jasper_report_dispatcher',
    default_args=default_args,
    description='Universal DAG to compile and dispatch Jasper reports',
    schedule=None,  # Triggered externally only
    catchup=False,
    is_paused_upon_creation=False,  # <--- CRITICAL: Prevents Airflow from leaving new instances paused
    tags=['jasper'],
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
