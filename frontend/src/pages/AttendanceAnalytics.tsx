import React, { useEffect, useState } from 'react';
import { BarChart3, Clock3, UserCheck, UserX, AlertTriangle, Users, LogOut } from 'lucide-react';
import { reportApi } from '../services/api';
import { AttendanceAnalytics as Analytics } from '../types';

const cardStyle: React.CSSProperties = { display: 'flex', alignItems: 'center', gap: '1rem' };

export const AttendanceAnalytics: React.FC = () => {
  const [data, setData] = useState<Analytics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    reportApi.getAnalytics()
      .then(response => setData(response.data))
      .catch(() => setError('Unable to load analytics. Please try again.'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="card">Loading attendance analytics...</div>;
  if (error || !data) return <div className="card" style={{ color: 'var(--danger)' }}>{error || 'No analytics available.'}</div>;
  const maxPresent = Math.max(1, ...data.dailyTrend.map(point => point.present));
  const maxHours = Math.max(1, ...data.employeeHours.map(employee => employee.totalMinutes));
  const metricCards = [
    { label: 'Active employees', value: data.totalEmployees, icon: <Users color="#2563eb" /> },
    { label: 'Present today', value: data.employeesPresent, icon: <UserCheck color="#16a34a" /> },
    { label: 'Absent today', value: data.employeesAbsent, icon: <UserX color="#dc2626" /> },
    { label: 'Late arrivals', value: data.lateArrivals, icon: <AlertTriangle color="#ca8a04" /> },
    { label: 'Currently clocked in', value: data.currentlyClockedIn, icon: <Clock3 color="#7c3aed" /> },
    { label: 'Clocked out today', value: data.currentlyClockedOut, icon: <LogOut color="#0891b2" /> }
  ];

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <header>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 700, display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
          <BarChart3 size={24} color="#2563eb" /> Attendance Analytics
        </h1>
        <p style={{ color: 'var(--text-muted)' }}>Live workforce attendance metrics for {data.date}.</p>
      </header>
      <div className="analytics-grid analytics-grid-six">
        {metricCards.map(card => <div className="card" style={cardStyle} key={card.label}>
          {card.icon}<div><div style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>{card.label}</div><strong style={{ fontSize: '1.5rem' }}>{card.value}</strong></div>
        </div>)}
      </div>
      <div className="card">
        <h2 style={{ fontSize: '1rem', marginBottom: '1rem' }}>Attendance percentage</h2>
        <div style={{ height: 12, background: '#e2e8f0', borderRadius: 99 }}>
          <div style={{ width: `${Math.min(100, data.attendancePercentage)}%`, height: '100%', background: '#2563eb', borderRadius: 99 }} />
        </div>
        <strong style={{ display: 'block', marginTop: '0.5rem' }}>{data.attendancePercentage}%</strong>
      </div>
      <div className="analytics-two-column">
        <div className="card">
          <h2 style={{ fontSize: '1rem', marginBottom: '1rem' }}>Daily attendance trend</h2>
          <div className="trend-chart">
            {data.dailyTrend.map(point => <div className="trend-column" key={point.date} title={`${point.date}: ${point.present} present`}>
              <div className="trend-bar" style={{ height: `${Math.max(4, point.present / maxPresent * 150)}px` }} />
              <small>{point.date.slice(5)}</small>
            </div>)}
          </div>
        </div>
        <div className="card">
          <h2 style={{ fontSize: '1rem', marginBottom: '1rem' }}>Working hours (last 30 days)</h2>
          {data.employeeHours.length === 0 ? <p style={{ color: 'var(--text-muted)' }}>No completed sessions yet.</p> :
            data.employeeHours.slice(0, 8).map(employee => <div key={employee.employeeCode} style={{ marginBottom: '0.75rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem' }}><span>{employee.employeeName}</span><strong>{Math.floor(employee.totalMinutes / 60)}h {employee.totalMinutes % 60}m</strong></div>
              <div style={{ height: 7, background: '#e2e8f0', borderRadius: 99, marginTop: 4 }}><div style={{ width: `${employee.totalMinutes / maxHours * 100}%`, height: '100%', background: '#7c3aed', borderRadius: 99 }} /></div>
            </div>)}
        </div>
      </div>
    </div>
  );
};
