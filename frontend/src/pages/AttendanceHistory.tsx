import React, { useState, useEffect, useCallback } from 'react';
import { attendanceApi, correctionApi } from '../services/api';
import { AttendanceRecord } from '../types';
import { Calendar, Filter, ChevronLeft, ChevronRight } from 'lucide-react';

export const AttendanceHistory: React.FC = () => {
  const [records, setRecords] = useState<AttendanceRecord[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [loading, setLoading] = useState(false);

  const fetchHistory = useCallback(async () => {
    setLoading(true);
    try {
      const res = await attendanceApi.getMyHistory({
        page,
        size: 15,
        startDate: startDate || undefined,
        endDate: endDate || undefined
      });
      setRecords(res.data.content);
      setTotalPages(res.data.totalPages);
      setTotalElements(res.data.totalElements);
    } catch (err) {
      console.error('Failed to load history', err);
    } finally {
      setLoading(false);
    }
  }, [page, startDate, endDate]);

  useEffect(() => {
    fetchHistory();
  }, [fetchHistory]);

  const handleFilterSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    fetchHistory();
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div>
        <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#0f172a' }}>
          My Attendance History
        </h1>
        <p style={{ color: '#64748b', fontSize: '0.875rem' }}>
          View and filter your personal past shift records and recorded durations.
        </p>
      </div>

      {/* Filter Bar */}
      <div className="card" style={{ padding: '1rem 1.5rem' }}>
        <form onSubmit={handleFilterSubmit} style={{ display: 'flex', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Calendar size={16} color="#64748b" />
            <span style={{ fontSize: '0.875rem', fontWeight: 500 }}>From:</span>
            <input
              type="date"
              className="input-field"
              style={{ width: '160px', padding: '0.4rem 0.6rem' }}
              value={startDate}
              onChange={e => setStartDate(e.target.value)}
            />
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <span style={{ fontSize: '0.875rem', fontWeight: 500 }}>To:</span>
            <input
              type="date"
              className="input-field"
              style={{ width: '160px', padding: '0.4rem 0.6rem' }}
              value={endDate}
              onChange={e => setEndDate(e.target.value)}
            />
          </div>

          <button type="submit" className="btn btn-primary" style={{ padding: '0.45rem 1rem' }}>
            <Filter size={16} />
            <span>Apply Filter</span>
          </button>

          {(startDate || endDate) && (
            <button
              type="button"
              onClick={() => { setStartDate(''); setEndDate(''); setPage(0); }}
              className="btn btn-outline"
              style={{ padding: '0.45rem 0.8rem' }}
            >
              Reset
            </button>
          )}

          <div style={{ marginLeft: 'auto', fontSize: '0.875rem', color: '#64748b' }}>
            Total Records: <strong>{totalElements}</strong>
          </div>
        </form>
      </div>

      {/* Table */}
      <div className="card">
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Clock In (UTC)</th>
                <th>Clock Out (UTC)</th>
                <th>Duration</th>
                <th>Status</th>
                <th>Notes</th>
                <th>Correction</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', color: '#64748b', padding: '2rem' }}>
                    Loading records...
                  </td>
                </tr>
              ) : records.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', color: '#64748b', padding: '2rem' }}>
                    No attendance records found for selected range.
                  </td>
                </tr>
              ) : (
                records.map(record => (
                  <tr key={record.id}>
                    <td><strong>{record.attendanceDate}</strong></td>
                    <td>{new Date(record.clockInTime).toLocaleTimeString()}</td>
                    <td>{record.clockOutTime ? new Date(record.clockOutTime).toLocaleTimeString() : '—'}</td>
                    <td>
                      {record.durationMinutes !== null ? (
                        <span>{Math.floor(record.durationMinutes / 60)}h {record.durationMinutes % 60}m</span>
                      ) : '—'}
                    </td>
                    <td>
                      <span className={`badge ${record.status === 'COMPLETED' ? 'badge-completed' : 'badge-open'}`}>
                        {record.status}
                      </span>
                    </td>
                    <td style={{ color: '#64748b', fontSize: '0.8rem' }}>{record.notes || '—'}</td>
                    <td>
                      <button className="btn btn-outline" style={{ padding: '0.35rem 0.6rem' }} onClick={async () => {
                        const reason = window.prompt('Why does this attendance record need correction?');
                        if (!reason?.trim()) return;
                        try {
                          await correctionApi.submit(record.id, {
                            proposedClockIn: record.clockInTime,
                            proposedClockOut: record.clockOutTime || undefined,
                            reason: reason.trim()
                          });
                          window.alert('Correction request submitted for manager review.');
                        } catch { window.alert('Unable to submit correction request.'); }
                      }}>Request correction</button>
                    </td>
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
