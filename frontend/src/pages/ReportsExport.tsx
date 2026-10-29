import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { reportApi } from '../services/api';
import { AttendanceSummary, DailyReport } from '../types';
import { BarChart3, Download, Calendar, CheckCircle2, Clock, Users, ArrowUpRight } from 'lucide-react';

export const ReportsExport: React.FC = () => {
  const { user } = useAuth();
  const isPrivileged = user?.role === 'ADMIN' || user?.role === 'MANAGER';

  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
  const [dailyReport, setDailyReport] = useState<DailyReport | null>(null);
  const [summary, setSummary] = useState<AttendanceSummary | null>(null);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [isExporting, setIsExporting] = useState(false);

  useEffect(() => {
    if (isPrivileged) {
      reportApi.getDaily(date)
        .then(res => setDailyReport(res.data))
        .catch(err => console.error('Failed to fetch daily report', err));
    }

    reportApi.getSummary({
      startDate: startDate || undefined,
      endDate: endDate || undefined
    })
      .then(res => setSummary(res.data))
      .catch(err => console.error('Failed to fetch summary', err));
  }, [date, startDate, endDate, isPrivileged]);

  const handleCsvExport = async () => {
    setIsExporting(true);
    try {
      await reportApi.downloadCsv({
        startDate: startDate || undefined,
        endDate: endDate || undefined
      });
    } catch (err) {
      alert('Failed to generate CSV export.');
      console.error(err);
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#0f172a', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <BarChart3 size={24} color="#2563eb" />
            <span>Attendance Reports & Analytics</span>
          </h1>
          <p style={{ color: '#64748b', fontSize: '0.875rem' }}>
            Comprehensive shift analytics, daily working hour aggregations, and spreadsheet exports.
          </p>
        </div>

        <button
          onClick={handleCsvExport}
          disabled={isExporting}
          className="btn btn-primary"
        >
          <Download size={18} />
          <span>{isExporting ? 'Generating CSV...' : 'Export to CSV'}</span>
        </button>
      </div>

      {/* Daily Overview KPI Cards (for Managers & Admins) */}
      {isPrivileged && (
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
            <h2 style={{ fontSize: '1.125rem', fontWeight: 600 }}>Daily Enterprise Snapshot</h2>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Calendar size={16} color="#64748b" />
              <input
                type="date"
                className="input-field"
                style={{ width: '160px', padding: '0.35rem 0.6rem' }}
                value={date}
                onChange={e => setDate(e.target.value)}
              />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '1rem' }}>
            <div style={{ backgroundColor: '#f8fafc', padding: '1.25rem', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', color: '#64748b', fontSize: '0.8rem', fontWeight: 600 }}>
                <span>TOTAL PRESENT</span>
                <Users size={16} color="#2563eb" />
              </div>
              <div style={{ fontSize: '1.75rem', fontWeight: 700, marginTop: '0.5rem' }}>
                {dailyReport?.totalPresent ?? 0}
              </div>
              <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.25rem' }}>
                Employees checked in today
              </div>
            </div>

            <div style={{ backgroundColor: '#f8fafc', padding: '1.25rem', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', color: '#64748b', fontSize: '0.8rem', fontWeight: 600 }}>
                <span>COMPLETED SHIFTS</span>
                <CheckCircle2 size={16} color="#16a34a" />
              </div>
              <div style={{ fontSize: '1.75rem', fontWeight: 700, marginTop: '0.5rem', color: '#16a34a' }}>
                {dailyReport?.completedSessions ?? 0}
              </div>
              <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.25rem' }}>
                Clocked out successfully
              </div>
            </div>

            <div style={{ backgroundColor: '#f8fafc', padding: '1.25rem', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', color: '#64748b', fontSize: '0.8rem', fontWeight: 600 }}>
                <span>ACTIVE (OPEN) SHIFTS</span>
                <Clock size={16} color="#ca8a04" />
              </div>
              <div style={{ fontSize: '1.75rem', fontWeight: 700, marginTop: '0.5rem', color: '#ca8a04' }}>
                {dailyReport?.openSessions ?? 0}
              </div>
              <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.25rem' }}>
                Currently working
              </div>
            </div>

            <div style={{ backgroundColor: '#f8fafc', padding: '1.25rem', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', color: '#64748b', fontSize: '0.8rem', fontWeight: 600 }}>
                <span>AVG SHIFT LENGTH</span>
                <ArrowUpRight size={16} color="#2563eb" />
              </div>
              <div style={{ fontSize: '1.75rem', fontWeight: 700, marginTop: '0.5rem' }}>
                {dailyReport ? `${Math.round(dailyReport.averageDurationMinutes / 60 * 10) / 10}h` : '0h'}
              </div>
              <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.25rem' }}>
                Across completed shifts
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Aggregate Range Summary Card */}
      <div className="card">
        <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem' }}>
          Date Range Summary Metrics
        </h2>
        <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span style={{ fontSize: '0.875rem' }}>Start Date:</span>
            <input
              type="date"
              className="input-field"
              style={{ width: '160px', padding: '0.35rem 0.6rem' }}
              value={startDate}
              onChange={e => setStartDate(e.target.value)}
            />
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span style={{ fontSize: '0.875rem' }}>End Date:</span>
            <input
              type="date"
              className="input-field"
              style={{ width: '160px', padding: '0.35rem 0.6rem' }}
              value={endDate}
              onChange={e => setEndDate(e.target.value)}
            />
          </div>
          {(startDate || endDate) && (
            <button onClick={() => { setStartDate(''); setEndDate(''); }} className="btn btn-outline" style={{ padding: '0.35rem 0.75rem' }}>
              Clear
            </button>
          )}
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '1rem' }}>
          <div style={{ border: '1px solid #e2e8f0', borderRadius: '8px', padding: '1rem' }}>
            <div style={{ fontSize: '0.75rem', color: '#64748b', fontWeight: 600 }}>TOTAL LOGGED SESSIONS</div>
            <div style={{ fontSize: '1.5rem', fontWeight: 700, marginTop: '0.25rem' }}>
              {summary?.totalSessions ?? 0}
            </div>
          </div>
          <div style={{ border: '1px solid #e2e8f0', borderRadius: '8px', padding: '1rem' }}>
            <div style={{ fontSize: '0.75rem', color: '#64748b', fontWeight: 600 }}>TOTAL COMPLETED WORKING HOURS</div>
            <div style={{ fontSize: '1.5rem', fontWeight: 700, marginTop: '0.25rem' }}>
              {summary ? `${Math.floor(summary.totalDurationMinutes / 60)}h ${summary.totalDurationMinutes % 60}m` : '0h 0m'}
            </div>
          </div>
          <div style={{ border: '1px solid #e2e8f0', borderRadius: '8px', padding: '1rem' }}>
            <div style={{ fontSize: '0.75rem', color: '#64748b', fontWeight: 600 }}>AVERAGE SESSION DURATION</div>
            <div style={{ fontSize: '1.5rem', fontWeight: 700, marginTop: '0.25rem' }}>
              {summary ? `${Math.floor(summary.averageDurationMinutes / 60)}h ${Math.round(summary.averageDurationMinutes % 60)}m` : '0h 0m'}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

