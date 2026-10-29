import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { attendanceApi } from '../services/api';
import { AttendanceRecord } from '../types';
import { ConfirmationModal } from '../components/ConfirmationModal';
import { Clock, CheckCircle2, Calendar } from 'lucide-react';
import { AxiosError } from 'axios';

export const EmployeeDashboard: React.FC = () => {
  const { user } = useAuth();
  const [todayRecord, setTodayRecord] = useState<AttendanceRecord | null>(null);
  const [recentRecords, setRecentRecords] = useState<AttendanceRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [notes, setNotes] = useState('');
  const [clockOutNotes, setClockOutNotes] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const [modalAction, setModalAction] = useState<'CLOCK_IN' | 'CLOCK_OUT' | null>(null);
  const [actionLoading, setActionLoading] = useState(false);

  const fetchDashboardData = async () => {
    setLoading(true);
    try {
      const todayStr = new Date().toISOString().slice(0, 10);
      const res = await attendanceApi.getMyHistory({ size: 5 });
      setRecentRecords(res.data.content);

      // Show today's record first, but surface an older unresolved open session
      // so it can be closed instead of leaving the employee blocked.
      const todayMatch = res.data.content.find(r => r.attendanceDate === todayStr);
      const openMatch = res.data.content.find(r => r.status === 'OPEN');
      setTodayRecord(todayMatch || openMatch || null);
    } catch {
      setError('Failed to fetch dashboard attendance status.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const handleConfirmAction = async () => {
    if (!modalAction) return;
    setActionLoading(true);
    setError(null);
    setSuccess(null);

    try {
      if (modalAction === 'CLOCK_IN') {
        const res = await attendanceApi.clockIn(notes || undefined);
        setTodayRecord(res.data);
        setSuccess('Successfully clocked in for today!');
      } else if (modalAction === 'CLOCK_OUT') {
        if (!clockOutNotes.trim()) {
          setError('Please enter what you worked on and your progress before clocking out.');
          setActionLoading(false);
          return;
        }
        const res = await attendanceApi.clockOut(clockOutNotes.trim());
        setTodayRecord(res.data);
        setSuccess(`Successfully clocked out at ${new Date(res.data.clockOutTime || '').toLocaleTimeString()}. Duration recorded.`);
      }
      setNotes('');
      setClockOutNotes('');
      setModalAction(null);
      await fetchDashboardData();
    } catch (err) {
      const axiosErr = err as AxiosError<{ detail?: string }>;
      setError(axiosErr.response?.data?.detail || 'Attendance action failed.');
      setModalAction(null);
    } finally {
      setActionLoading(false);
    }
  };

  const isClockedIn = todayRecord?.status === 'OPEN';
  const isCompletedToday = todayRecord?.status === 'COMPLETED';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#0f172a' }}>
          Welcome back, {user?.firstName}
        </h1>
        <p style={{ color: '#64748b', fontSize: '0.875rem' }}>
          Here is your daily attendance status and shift overview.
        </p>
      </div>

      {error && (
        <div style={{ backgroundColor: '#fee2e2', border: '1px solid #f87171', color: '#b91c1c', padding: '0.75rem 1rem', borderRadius: '8px', fontSize: '0.875rem' }}>
          {error}
        </div>
      )}

      {success && (
        <div style={{ backgroundColor: '#dcfce7', border: '1px solid #86efac', color: '#15803d', padding: '0.75rem 1rem', borderRadius: '8px', fontSize: '0.875rem' }}>
          {success}
        </div>
      )}

      {/* Clock In / Out Action Widget */}
      <div className="card" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem' }}>
        <div>
          <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Clock size={20} color="#2563eb" />
            <span>Shift Action Terminal</span>
          </h2>
          <p style={{ fontSize: '0.875rem', color: '#64748b', marginBottom: '1.25rem' }}>
            Server timestamps govern shift records in accordance with corporate time policy.
          </p>

          <div style={{ marginBottom: '1rem' }}>
            <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 500, color: '#475569', marginBottom: '0.25rem' }}>
              Clock-in note (Optional)
            </label>
            <input
              type="text"
              placeholder="e.g. Remote work, client meeting"
              className="input-field"
              value={notes}
              onChange={e => setNotes(e.target.value)}
              disabled={isCompletedToday}
            />
          </div>

          <div style={{ display: 'flex', gap: '1rem' }}>
            {!isClockedIn && !isCompletedToday && (
              <button
                type="button"
                onClick={() => setModalAction('CLOCK_IN')}
                disabled={actionLoading}
                className="btn btn-primary"
                style={{ flex: 1 }}
              >
                <CheckCircle2 size={18} />
                <span>Clock In Now</span>
              </button>
            )}

            {isClockedIn && (
              <button
                type="button"
                onClick={() => { setClockOutNotes(''); setModalAction('CLOCK_OUT'); }}
                disabled={actionLoading}
                className="btn btn-danger"
                style={{ flex: 1 }}
              >
                <Clock size={18} />
                <span>Clock Out & Save Progress</span>
              </button>
            )}

            {isCompletedToday && (
              <div style={{
                padding: '0.75rem 1rem',
                backgroundColor: '#f1f5f9',
                borderRadius: '8px',
                color: '#475569',
                fontSize: '0.875rem',
                fontWeight: 500,
                width: '100%',
                textAlign: 'center'
              }}>
                ✓ Daily Attendance Completed
              </div>
            )}
          </div>
        </div>

        {/* Status Card */}
        <div style={{
          backgroundColor: '#f8fafc',
          border: '1px solid #e2e8f0',
          borderRadius: '8px',
          padding: '1.25rem',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between'
        }}>
          <div>
            <div style={{ fontSize: '0.75rem', fontWeight: 600, color: '#64748b', textTransform: 'uppercase' }}>
              Today's Status
            </div>
            <div style={{ marginTop: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              {isClockedIn && <span className="badge badge-open">Shift In Progress (Open)</span>}
              {isCompletedToday && <span className="badge badge-completed">Shift Completed</span>}
              {!todayRecord && <span className="badge badge-inactive">Not Clocked In Yet</span>}
            </div>

            {todayRecord && (
              <div style={{ marginTop: '1rem', fontSize: '0.875rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                <div>
                  <span style={{ color: '#64748b' }}>Clock-In Time: </span>
                  <strong>{new Date(todayRecord.clockInTime).toLocaleTimeString()}</strong>
                </div>
                {todayRecord.clockOutTime && (
                  <div>
                    <span style={{ color: '#64748b' }}>Clock-Out Time: </span>
                    <strong>{new Date(todayRecord.clockOutTime).toLocaleTimeString()}</strong>
                  </div>
                )}
                {todayRecord.durationMinutes !== null && (
                  <div>
                    <span style={{ color: '#64748b' }}>Recorded Duration: </span>
                    <strong>{Math.floor(todayRecord.durationMinutes / 60)}h {todayRecord.durationMinutes % 60}m</strong>
                  </div>
                )}
              </div>
            )}
          </div>

          <div style={{ fontSize: '0.75rem', color: '#94a3b8', display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
            <Calendar size={14} />
            <span>Attendance Date: {todayRecord?.attendanceDate || new Date().toLocaleDateString(undefined, { weekday: 'long', year: 'numeric', month: 'short', day: 'numeric' })}</span>
          </div>
        </div>
      </div>

      {/* Recent Attendance History Table */}
      <div className="card">
        <h2 style={{ fontSize: '1.125rem', fontWeight: 600, marginBottom: '1rem' }}>
          Recent Activity
        </h2>
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Clock In</th>
                <th>Clock Out</th>
                <th>Duration</th>
                <th>Status</th>
                <th>Notes</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', color: '#64748b', padding: '2rem' }}>
                    Loading attendance records...
                  </td>
                </tr>
              ) : recentRecords.length === 0 ? (
                <tr>
                  <td colSpan={6} style={{ textAlign: 'center', color: '#64748b', padding: '2rem' }}>
                    No recent attendance records found.
                  </td>
                </tr>
              ) : (
                recentRecords.map(record => (
                  <tr key={record.id}>
                    <td><strong>{record.attendanceDate}</strong></td>
                    <td>{new Date(record.clockInTime).toLocaleTimeString()}</td>
                    <td>{record.clockOutTime ? new Date(record.clockOutTime).toLocaleTimeString() : '—'}</td>
                    <td>{record.durationMinutes !== null ? `${Math.floor(record.durationMinutes / 60)}h ${record.durationMinutes % 60}m` : '—'}</td>
                    <td>
                      <span className={`badge ${record.status === 'COMPLETED' ? 'badge-completed' : 'badge-open'}`}>
                        {record.status}
                      </span>
                    </td>
                    <td style={{ color: '#64748b', fontSize: '0.8rem' }}>{record.notes || '—'}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal Dialog */}
      <ConfirmationModal
        isOpen={!!modalAction}
        title={modalAction === 'CLOCK_IN' ? 'Confirm Clock-In' : 'Confirm Clock-Out'}
        message={
          modalAction === 'CLOCK_IN'
            ? 'Are you sure you want to clock in now? A new attendance record will be opened for today.'
            : 'Are you sure you want to clock out? Your session will be closed and working hours will be calculated.'
        }
        confirmLabel={modalAction === 'CLOCK_IN' ? 'Yes, Clock In' : 'Yes, Clock Out'}
        onConfirm={handleConfirmAction}
        onCancel={() => setModalAction(null)}
      >
        {modalAction === 'CLOCK_OUT' && (
          <div style={{ marginBottom: '1.5rem' }}>
            {todayRecord && (
              <div style={{ backgroundColor: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '0.75rem', marginBottom: '1rem', fontSize: '0.875rem' }}>
                <div><span style={{ color: '#64748b' }}>Clocked in at: </span><strong>{new Date(todayRecord.clockInTime).toLocaleTimeString()}</strong></div>
                <div style={{ color: '#64748b', marginTop: '0.25rem' }}>Clock-out time and total duration will be recorded when you confirm.</div>
              </div>
            )}
            <label htmlFor="clock-out-progress" style={{ display: 'block', fontSize: '0.875rem', fontWeight: 600, marginBottom: '0.4rem' }}>
              Today's work and progress <span style={{ color: '#dc2626' }}>*</span>
            </label>
            <textarea
              id="clock-out-progress"
              className="input-field"
              rows={4}
              maxLength={500}
              placeholder="Describe what you worked on, what you completed, and any progress or follow-up for tomorrow..."
              value={clockOutNotes}
              onChange={event => setClockOutNotes(event.target.value)}
              autoFocus
            />
            <div style={{ color: '#64748b', fontSize: '0.75rem', marginTop: '0.25rem' }}>
              {clockOutNotes.length}/500 characters
            </div>
          </div>
        )}
      </ConfirmationModal>
    </div>
  );
};
