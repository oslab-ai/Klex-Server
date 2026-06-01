import { Routes, Route, Navigate } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import Layout from './components/Layout'
import Login from './pages/Login'
import AdminSetup from './pages/AdminSetup'
import Reports from './pages/Reports'
import ReportViewer from './pages/ReportViewer'
import Users from './pages/Users'
import DataAdapters from './pages/DataAdapters'
import Permissions from './pages/Permissions'
import Schedules from './pages/Schedules'
import Embeddings from './pages/Embeddings'
import AirflowRedirect from './pages/AirflowRedirect'
import DataExploration from './pages/DataExploration'

function ProtectedRoute({ children, adminOnly = false }: { children: React.ReactNode; adminOnly?: boolean }) {
    const { user, loading } = useAuth()

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="w-8 h-8 border-4 border-primary-500 border-t-transparent rounded-full animate-spin"></div>
            </div>
        )
    }

    if (!user) {
        return <Navigate to="/login" replace />
    }

    if (adminOnly && !user.is_admin) {
        return <Navigate to="/" replace />
    }

    return <>{children}</>
}

function App() {
    const { user, loading } = useAuth()

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-surface-50 dark:bg-surface-900">
                <div className="text-center">
                    <div className="w-12 h-12 border-4 border-primary-500 border-t-transparent rounded-full animate-spin mx-auto"></div>
                    <p className="mt-4 text-gray-500 dark:text-gray-400">Loading...</p>
                </div>
            </div>
        )
    }

    return (
        <Routes>
            <Route path="/login" element={user ? <Navigate to="/" replace /> : <Login />} />
            <Route path="/admin-setup" element={<AdminSetup />} />

            <Route path="/" element={
                <ProtectedRoute>
                    <Layout />
                </ProtectedRoute>
            }>
                <Route index element={<Reports />} />
                <Route path="reports/:id" element={<ReportViewer />} />
                <Route path="embeddings" element={<Embeddings />} />
                <Route path="explore" element={
                    <ProtectedRoute adminOnly>
                        <DataExploration />
                    </ProtectedRoute>
                } />
                <Route path="users" element={
                    <ProtectedRoute adminOnly>
                        <Users />
                    </ProtectedRoute>
                } />
                <Route path="data-adapters" element={
                    <ProtectedRoute adminOnly>
                        <DataAdapters />
                    </ProtectedRoute>
                } />
                <Route path="permissions" element={
                    <ProtectedRoute adminOnly>
                        <Permissions />
                    </ProtectedRoute>
                } />
                <Route path="schedules" element={
                    <ProtectedRoute adminOnly>
                        <Schedules />
                    </ProtectedRoute>
                } />
                <Route path="airflow-dags" element={
                    <ProtectedRoute adminOnly>
                        <AirflowRedirect />
                    </ProtectedRoute>
                } />
            </Route>

            <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
    )
}

export default App
