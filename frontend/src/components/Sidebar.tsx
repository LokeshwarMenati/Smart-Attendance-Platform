import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { LayoutDashboard, History, Users, BarChart3, ShieldCheck, ClipboardEdit } from 'lucide-react';

export const Sidebar: React.FC = () => {
  const { user } = useAuth();

  const navItemStyle = ({ isActive }: { isActive: boolean }) => ({
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
    padding: '0.75rem 1rem',
    borderRadius: '8px',
    fontSize: '0.875rem',
    fontWeight: 500,
    color: isActive ? '#2563eb' : '#475569',
    backgroundColor: isActive ? '#eff6ff' : 'transparent',
    transition: 'all 0.15s ease'
  });

  return (
    <aside style={{
      width: '240px',
      backgroundColor: '#ffffff',
      borderRight: '1px solid #e2e8f0',
      minHeight: 'calc(100vh - 64px)',
      padding: '1.25rem 0.75rem',
      display: 'flex',
      flexDirection: 'column',
      gap: '0.5rem'
    }}>
      <div style={{ fontSize: '0.75rem', fontWeight: 600, color: '#94a3b8', padding: '0.5rem 1rem', textTransform: 'uppercase' }}>
        Attendance
      </div>

      <NavLink to="/dashboard" style={navItemStyle}>
        <LayoutDashboard size={18} />
        <span>My Dashboard</span>
      </NavLink>

      <NavLink to="/history" style={navItemStyle}>
        <History size={18} />
        <span>My History</span>
      </NavLink>
      <NavLink to="/corrections" style={navItemStyle}>
        <ClipboardEdit size={18} />
        <span>Correction Requests</span>
      </NavLink>

      {(user?.role === 'MANAGER' || user?.role === 'ADMIN') && (
        <>
          <div style={{ fontSize: '0.75rem', fontWeight: 600, color: '#94a3b8', padding: '1rem 1rem 0.5rem 1rem', textTransform: 'uppercase' }}>
            Management
          </div>
          <NavLink to="/manager/attendance" style={navItemStyle}>
            <Users size={18} />
            <span>Team Attendance</span>
          </NavLink>
          <NavLink to="/manager/analytics" style={navItemStyle}>
            <BarChart3 size={18} />
            <span>Attendance Analytics</span>
          </NavLink>
        </>
      )}

      {(user?.role === 'ADMIN' || user?.role === 'MANAGER') && (
        <NavLink to="/admin/employees" style={navItemStyle}>
          <ShieldCheck size={18} />
          <span>Employees Admin</span>
        </NavLink>
      )}

      <div style={{ fontSize: '0.75rem', fontWeight: 600, color: '#94a3b8', padding: '1rem 1rem 0.5rem 1rem', textTransform: 'uppercase' }}>
        Analytics
      </div>

      <NavLink to="/reports" style={navItemStyle}>
        <BarChart3 size={18} />
        <span>Reports & Export</span>
      </NavLink>
    </aside>
  );
};
