import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { jobsApi } from '../api';

export default function AirflowRedirect() {
    const navigate = useNavigate();

    useEffect(() => {
        jobsApi.getAirflowUiUrl().then(res => {
            if (res.available && res.dags_url) {
                window.location.href = res.dags_url;
            } else {
                alert(res.message || 'Airflow UI is not available.');
                navigate('/schedules', { replace: true });
            }
        }).catch(() => {
            alert('Failed to get Airflow UI URL.');
            navigate('/schedules', { replace: true });
        });
    }, [navigate]);

    return (
        <div className="flex items-center justify-center min-h-[50vh]">
            <div className="text-center">
                <div className="w-8 h-8 border-4 border-primary-500 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
                <p className="text-gray-500 dark:text-gray-400">Redirecting to Airflow...</p>
            </div>
        </div>
    );
}
