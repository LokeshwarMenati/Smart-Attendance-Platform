import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { reportApi, attendanceApi } from '../services/api';
import { AttendanceRecord, AttendanceSummary, DailyReport } from '../types';
import { BarChart3, Download, Calendar, CheckCircle2, Clock, Users, ArrowUpRight, TrendingUp, Target } from 'lucide-react';

export const ReportsExport: React.FC = () => {
  const { user } = useAuth();
  const isPrivileged = user?.role === 'ADMIN' || user?.role === 'MANAGER';

  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
  const [dailyReport, setDailyReport] = useState<DailyReport | null>(null);
  const [summary, setSummary] = useState<AttendanceSummary | null>(null);
  const [records, setRecords] = useState<AttendanceRecord[]>([]);
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

    const recordsPromise = isPrivileged
      ? attendanceApi.listAttendance({ startDate: startDate || undefined, endDate: endDate || undefined, size: 20 })
      : attendanceApi.getMyHistory({ startDate: startDate || undefined, endDate: endDate || undefined, size: 20 });

    recordsPromise
      .then((res: { data: { content: AttendanceRecord[] } }) => setRecords(res.data.content))
      .catch((err: unknown) => console.error('Failed to fetch report records', err));
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

      {/* Visual Analytics & Attendance Charts */}
      {records.length > 0 && (
        <div className="analytics-two-column">
          {/* Daily Work Hours Trend Chart */}
          <div className="card">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <div>
                <h2 style={{ fontSize: '1rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <TrendingUp size={18} color="#2563eb" />
                  <span>Daily Shift Duration Trend</span>
                </h2>
                <p style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.2rem' }}>
                  Recorded hours per shift across logged sessions
                </p>
              </div>
              <span className="badge badge-active" style={{ fontSize: '0.7rem' }}>
                Standard: 8.0h/day
              </span>
            </div>

            <div style={{ height: '180px', display: 'flex', alignItems: 'flex-end', justifyContent: 'space-around', gap: '0.75rem', paddingBottom: '0.5rem', borderBottom: '1px solid #e2e8f0' }}>
              {[...records].reverse().map(rec => {
                const durationHrs = rec.durationMinutes ? Math.round((rec.durationMinutes / 60) * 10) / 10 : 0;
                const maxBarHeight = 130;
                const barHeight = Math.max(12, Math.min(maxBarHeight, (durationHrs / 10) * maxBarHeight));
                const isCompleted = rec.status === 'COMPLETED';

                return (
                  <div key={rec.id} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.35rem', flex: 1 }} title={`${rec.attendanceDate}: ${durationHrs}h (${rec.status})`}>
                    <span style={{ fontSize: '0.7rem', fontWeight: 600, color: isCompleted ? '#16a34a' : '#ca8a04' }}>
                      {durationHrs > 0 ? `${durationHrs}h` : '—'}
                    </span>
                    <div
                      style={{
                        width: '100%',
                        maxWidth: '36px',
                        height: `${barHeight}px`,
                        backgroundColor: isCompleted ? '#2563eb' : '#fbbf24',
                        borderRadius: '6px 6px 0 0',
                        transition: 'height 0.4s ease, background-color 0.2s',
                        boxShadow: '0 2px 4px rgba(37, 99, 235, 0.15)'
                      }}
                    />
                    <small style={{ fontSize: '0.7rem', color: '#64748b' }}>
                      {rec.attendanceDate.slice(5)}
                    </small>
                  </div>
                );
              })}
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '0.75rem', fontSize: '0.75rem', color: '#64748b' }}>
              <span style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                <span style={{ width: '8px', height: '8px', borderRadius: '2px', backgroundColor: '#2563eb', display: 'inline-block' }} />
                Completed Session
              </span>
              <span>Showing last {records.length} shifts</span>
            </div>
          </div>

          {/* Goal Completion & Work Hour Target */}
          <div className="card" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
                <div>
                  <h2 style={{ fontSize: '1rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <Target size={18} color="#16a34a" />
                    <span>Weekly Target & Efficiency</span>
                  </h2>
                  <p style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.2rem' }}>
                    Target: 40.0 hours / week commitment
                  </p>
                </div>
                <span className="badge badge-completed">
                  {summary && summary.totalDurationMinutes >= 2400 ? 'Goal Met' : 'In Progress'}
                </span>
              </div>

              {/* Progress bar */}
              <div style={{ marginBottom: '1.5rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', marginBottom: '0.4rem' }}>
                  <span style={{ color: '#64748b' }}>Accumulated Hours</span>
                  <strong>{summary ? `${Math.floor(summary.totalDurationMinutes / 60)}h ${summary.totalDurationMinutes % 60}m` : '0h'} / 40h</strong>
                </div>
                <div style={{ height: '14px', backgroundColor: '#e2e8f0', borderRadius: '9999px', overflow: 'hidden' }}>
                  <div
                    style={{
                      height: '100%',
                      width: `${Math.min(100, summary ? (summary.totalDurationMinutes / 2400) * 100 : 0)}%`,
                      backgroundColor: summary && summary.totalDurationMinutes >= 2400 ? '#16a34a' : '#2563eb',
                      borderRadius: '9999px',
                      transition: 'width 0.6s cubic-bezier(0.4, 0, 0.2, 1)'
                    }}
                  />
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.7rem', color: '#94a3b8', marginTop: '0.25rem' }}>
                  <span>0h</span>
                  <span>20h</span>
                  <span>40h</span>
                </div>
              </div>
            </div>

            {/* Sub-metrics stats grid */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', marginTop: 'auto' }}>
              <div style={{ padding: '0.75rem', backgroundColor: '#f8fafc', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                <div style={{ fontSize: '0.7rem', color: '#64748b', fontWeight: 600 }}>COMPLETION RATE</div>
                <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#16a34a', marginTop: '0.2rem' }}>
                  {summary && summary.totalSessions > 0
                    ? `${Math.round((summary.completedSessions / summary.totalSessions) * 100)}%`
                    : '100%'}
                </div>
                <div style={{ fontSize: '0.7rem', color: '#64748b' }}>{summary?.completedSessions ?? 0} of {summary?.totalSessions ?? 0} closed</div>
              </div>

              <div style={{ padding: '0.75rem', backgroundColor: '#f8fafc', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                <div style={{ fontSize: '0.7rem', color: '#64748b', fontWeight: 600 }}>AVG SHIFT DURATION</div>
                <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#2563eb', marginTop: '0.2rem' }}>
                  {summary ? `${Math.floor(summary.averageDurationMinutes / 60)}h ${Math.round(summary.averageDurationMinutes % 60)}m` : '0h'}
                </div>
                <div style={{ fontSize: '0.7rem', color: '#64748b' }}>Per logged work session</div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Attendance Session Records Table */}
      <div className="card">
        <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem' }}>
          Attendance Log Breakdown ({records.length} records)
        </h2>
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Employee</th>
                <th>Clock In</th>
                <th>Clock Out</th>
                <th>Duration</th>
                <th>Status</th>
                <th>Notes</th>
              </tr>
            </thead>
            <tbody>
              {records.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', color: '#64748b', padding: '2rem' }}>
                    No attendance records found for the selected criteria.
                  </td>
                </tr>
              ) : (
                records.map(rec => (
                  <tr key={rec.id}>
                    <td><strong>{rec.attendanceDate}</strong></td>
                    <td>{rec.employeeName} ({rec.employeeCode})</td>
                    <td>{new Date(rec.clockInTime).toLocaleTimeString()}</td>
                    <td>{rec.clockOutTime ? new Date(rec.clockOutTime).toLocaleTimeString() : '—'}</td>
                    <td>
                      {rec.durationMinutes !== null ? (
                        <strong>{Math.floor(rec.durationMinutes / 60)}h {rec.durationMinutes % 60}m</strong>
                      ) : '—'}
                    </td>
                    <td>
                      <span className={`badge ${rec.status === 'COMPLETED' ? 'badge-completed' : 'badge-open'}`}>
                        {rec.status}
                      </span>
                    </td>
                    <td style={{ color: '#64748b' }}>{rec.notes || '—'}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

