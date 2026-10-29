import React, { useState, useEffect, useCallback } from 'react';
import { attendanceApi } from '../services/api';
import { AttendanceRecord } from '../types';
import { Users, ChevronLeft, ChevronRight } from 'lucide-react';

export const ManagerDashboard: React.FC = () => {
  const [records, setRecords] = useState<AttendanceRecord[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [date, setDate] = useState(new Date().toISOString().slice(0, 10));
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(false);

  const fetchTeamAttendance = useCallback(async () => {
    setLoading(true);
    try {
      const res = await attendanceApi.listAttendance({
        page,
        size: 15,
        date: date || undefined,
        status: statusFilter || undefined
        , search: search || undefined
      });
      setRecords(res.data.content);
      setTotalPages(res.data.totalPages);
      setTotalElements(res.data.totalElements);
    } catch (err) {
      console.error('Failed to load team attendance', err);
    } finally {
      setLoading(false);
    }
  }, [page, date, statusFilter, search]);

  useEffect(() => {
    fetchTeamAttendance();
  }, [fetchTeamAttendance]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#0f172a', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Users size={24} color="#2563eb" />
          <span>Team Attendance Overview</span>
        </h1>
        <p style={{ color: '#64748b', fontSize: '0.875rem' }}>
          Monitor real-time employee attendance status, active shifts, and completed hours.
        </p>
      </div>

      {/* Filter Bar */}
      <div className="card" style={{ padding: '1rem 1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span style={{ fontSize: '0.875rem', fontWeight: 500 }}>Search:</span>
            <input className="input-field" placeholder="Name or code" style={{ width: '160px', padding: '0.4rem 0.6rem' }} value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} />
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span style={{ fontSize: '0.875rem', fontWeight: 500 }}>Date:</span>
            <input
              type="date"
              className="input-field"
              style={{ width: '160px', padding: '0.4rem 0.6rem' }}
              value={date}
              onChange={e => { setDate(e.target.value); setPage(0); }}
            />
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span style={{ fontSize: '0.875rem', fontWeight: 500 }}>Status:</span>
            <select
              className="input-field"
              style={{ width: '140px', padding: '0.4rem 0.6rem' }}
              value={statusFilter}
              onChange={e => { setStatusFilter(e.target.value); setPage(0); }}
            >
              <option value="">All Statuses</option>
              <option value="OPEN">OPEN (Shift Active)</option>
              <option value="COMPLETED">COMPLETED</option>
            </select>
          </div>

          <div style={{ marginLeft: 'auto', fontSize: '0.875rem', color: '#64748b' }}>
            Total Records: <strong>{totalElements}</strong>
          </div>
        </div>
      </div>

      {/* Table */}
      <div className="card">
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Employee</th>
                <th>Code</th>
                <th>Date</th>
                <th>Clock In</th>
                <th>Clock Out</th>
                <th>Duration</th>
                <th>Status</th>
                <th>Notes</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={9} style={{ textAlign: 'center', color: '#64748b', padding: '2rem' }}>
                    Loading team attendance...
                  </td>
                </tr>
              ) : records.length === 0 ? (
                <tr>
                  <td colSpan={9} style={{ textAlign: 'center', color: '#64748b', padding: '2rem' }}>
                    No attendance records found for criteria.
                  </td>
                </tr>
              ) : (
                records.map(record => (
                  <tr key={record.id}>
                    <td><strong>{record.employeeName}</strong></td>
                    <td><span style={{ fontFamily: 'monospace', color: '#475569' }}>{record.employeeCode}</span></td>
                    <td>{record.attendanceDate}</td>
                    <td>{new Date(record.clockInTime).toLocaleTimeString()}</td>
                    <td>{record.clockOutTime ? new Date(record.clockOutTime).toLocaleTimeString() : '—'}</td>
                    <td>{record.durationMinutes !== null ? `${Math.floor(record.durationMinutes / 60)}h ${record.durationMinutes % 60}m` : '—'}</td>
                    <td>
                      <span className={`badge ${record.status === 'COMPLETED' ? 'badge-completed' : 'badge-open'}`}>
                        {record.status}
                      </span>
                    </td>
                    <td style={{ color: '#64748b', fontSize: '0.8rem' }}>{record.notes || '—'}</td>
                    <td><button className="btn btn-outline" style={{ padding: '0.35rem 0.6rem' }} onClick={async () => {
                      const reason = window.prompt('Mandatory correction reason:');
                      if (!reason?.trim()) return;
                      await attendanceApi.correct(record.id, { reason: reason.trim(), clockInTime: record.clockInTime, clockOutTime: record.clockOutTime || undefined });
                      fetchTeamAttendance();
                    }}>Correct</button></td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Bar */}
        {totalPages > 1 && (
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '1rem' }}>
            <span style={{ fontSize: '0.875rem', color: '#64748b' }}>
              Page {page + 1} of {totalPages}
            </span>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
                className="btn btn-outline"
                style={{ padding: '0.4rem 0.75rem' }}
              >
                <ChevronLeft size={16} />
                <span>Prev</span>
              </button>
              <button
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="btn btn-outline"
                style={{ padding: '0.4rem 0.75rem' }}
              >
                <span>Next</span>
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
