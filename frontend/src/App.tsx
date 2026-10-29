import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Navbar } from './components/Navbar';
import { Sidebar } from './components/Sidebar';
import { Login } from './pages/Login';
import { EmployeeDashboard } from './pages/EmployeeDashboard';
import { AttendanceHistory } from './pages/AttendanceHistory';
import { ManagerDashboard } from './pages/ManagerDashboard';
import { AdminEmployees } from './pages/AdminEmployees';
import { ReportsExport } from './pages/ReportsExport';
import { AccessDenied } from './pages/AccessDenied';
import { NotFound } from './pages/NotFound';
import { AttendanceAnalytics } from './pages/AttendanceAnalytics';
import { CorrectionRequests } from './pages/CorrectionRequests';

const MainLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
    <Navbar />
    <div style={{ display: 'flex', flex: 1 }}>
      <Sidebar />
      <main style={{ flex: 1, padding: '2rem', maxWidth: '1200px' }}>
        {children}
      </main>
    </div>
  </div>
);

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />

          {/* Protected Routes */}
          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard" element={<MainLayout><EmployeeDashboard /></MainLayout>} />
            <Route path="/history" element={<MainLayout><AttendanceHistory /></MainLayout>} />
            <Route path="/reports" element={<MainLayout><ReportsExport /></MainLayout>} />
            <Route path="/corrections" element={<MainLayout><CorrectionRequests /></MainLayout>} />
          </Route>

          {/* Manager & Admin Routes */}
          <Route element={<ProtectedRoute allowedRoles={['MANAGER', 'ADMIN']} />}>
            <Route path="/manager/attendance" element={<MainLayout><ManagerDashboard /></MainLayout>} />
            <Route path="/manager/analytics" element={<MainLayout><AttendanceAnalytics /></MainLayout>} />
          </Route>

          {/* Admin Exclusive Routes */}
          <Route element={<ProtectedRoute allowedRoles={['MANAGER', 'ADMIN']} />}>
            <Route path="/admin/employees" element={<MainLayout><AdminEmployees /></MainLayout>} />
          </Route>

          <Route path="/access-denied" element={<MainLayout><AccessDenied /></MainLayout>} />
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<MainLayout><NotFound /></MainLayout>} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
