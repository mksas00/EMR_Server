import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

import { AuthProvider } from './contexts/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Layout } from './components/Layout';

import LoginPage from './pages/auth/LoginPage';
import DashboardPage from './pages/DashboardPage';
import PatientsPage from './pages/patients/PatientsPage';
import PatientDetailPage from './pages/patients/PatientDetailPage';
import PatientPrescriptionsPage from './pages/patients/PatientPrescriptionsPage';
import PatientLabResultsPage from './pages/patients/PatientLabResultsPage';
import VisitsPage from './pages/visits/VisitsPage';
import PrescriptionsPage from './pages/prescriptions/PrescriptionsPage';
import LabResultsPage from './pages/lab/LabResultsPage';
import LabOrdersPage from './pages/lab/LabOrdersPage';
import LabOrderDetailPage from './pages/lab/LabOrderDetailPage';
import ConsentsMePage from './pages/consents/ConsentsMePage';
import ProfilePage from './pages/profile/ProfilePage';
import MedicationsPage from './pages/medications/MedicationsPage';
import AdminDashboardPage from './pages/admin/AdminDashboardPage';
import AdminAuditLogPage from './pages/admin/AdminAuditLogPage';
import AdminIncidentsPage from './pages/admin/AdminIncidentsPage';
import AdminLoginAttemptsPage from './pages/admin/AdminLoginAttemptsPage';
import AdminSessionsPage from './pages/admin/AdminSessionsPage';
import AdminUsersPage from './pages/admin/AdminUsersPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: (failureCount, error) => {
        if (error?.response?.status === 401 || error?.response?.status === 403) return false;
        return failureCount < 2;
      },
    },
  },
});

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={
        <ProtectedRoute>
          <Layout>
            <DashboardPage />
          </Layout>
        </ProtectedRoute>
      } />
      <Route path="/patients" element={
        <ProtectedRoute roles={['admin', 'doctor', 'nurse']}>
          <Layout><PatientsPage /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/patients/:id" element={
        <ProtectedRoute roles={['admin', 'doctor', 'nurse']}>
          <Layout><PatientDetailPage /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/patients/:patientId/prescriptions" element={
        <ProtectedRoute roles={['admin', 'doctor', 'nurse']}>
          <Layout><PatientPrescriptionsPage /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/patients/:patientId/lab-results" element={
        <ProtectedRoute roles={['admin', 'doctor', 'nurse']}>
          <Layout><PatientLabResultsPage /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/visits" element={
        <ProtectedRoute>
          <Layout><VisitsPage /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/prescriptions" element={
        <ProtectedRoute>
          <Layout><PrescriptionsPage /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/lab-results" element={
        <ProtectedRoute>
          <Layout><LabResultsPage /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/lab-orders" element={
        <ProtectedRoute>
          <Layout><LabOrdersPage /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/lab-orders/:id" element={
        <ProtectedRoute>
          <Layout><LabOrderDetailPage /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/consents/mine" element={
        <ProtectedRoute>
          <Layout><ConsentsMePage /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/profile" element={
        <ProtectedRoute>
          <Layout><ProfilePage /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/medications" element={
        <ProtectedRoute>
          <Layout><MedicationsPage /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/admin" element={
        <ProtectedRoute roles={['admin']}>
          <Layout><AdminDashboardPage /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/admin/audit-log" element={
        <ProtectedRoute roles={['admin']}>
          <Layout><AdminAuditLogPage /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/admin/incidents" element={
        <ProtectedRoute roles={['admin']}>
          <Layout><AdminIncidentsPage /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/admin/login-attempts" element={
        <ProtectedRoute roles={['admin']}>
          <Layout><AdminLoginAttemptsPage /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/admin/sessions" element={
        <ProtectedRoute roles={['admin']}>
          <Layout><AdminSessionsPage /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/admin/users" element={
        <ProtectedRoute roles={['admin']}>
          <Layout><AdminUsersPage /></Layout>
        </ProtectedRoute>
      } />
      <Route path="/403" element={
        <div className="min-h-screen flex items-center justify-center bg-gray-50">
          <div className="text-center">
            <p className="text-6xl font-bold text-gray-200">403</p>
            <p className="text-xl font-semibold text-gray-700 mt-2">Brak dostępu</p>
            <p className="text-gray-500 mt-1">Nie masz uprawnień do tej strony.</p>
            <a href="/" className="btn-primary inline-flex mt-4">Wróć do pulpitu</a>
          </div>
        </div>
      } />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <AppRoutes />
          <ToastContainer
            position="bottom-right"
            autoClose={4000}
            hideProgressBar={false}
            closeOnClick
            pauseOnHover
            theme="light"
          />
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  );
}

