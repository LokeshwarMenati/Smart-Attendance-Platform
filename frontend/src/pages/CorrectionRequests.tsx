import React, { useEffect, useState } from 'react';
import { Check, ClipboardEdit, X } from 'lucide-react';
import { correctionApi } from '../services/api';
import { CorrectionRequest } from '../types';
import { useAuth } from '../context/AuthContext';

export const CorrectionRequests: React.FC = () => {
  const { user } = useAuth();
  const privileged = user?.role === 'MANAGER' || user?.role === 'ADMIN';
  const [requests, setRequests] = useState<CorrectionRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const load = () => correctionApi.list({ status: privileged ? 'PENDING' : undefined })
    .then(response => setRequests(response.data.content))
    .catch(() => setError('Unable to load correction requests.'))
    .finally(() => setLoading(false));
  useEffect(() => { load(); }, [privileged]);

  const decide = async (request: CorrectionRequest, approve: boolean) => {
    const reason = window.prompt(`Mandatory ${approve ? 'approval' : 'rejection'} reason:`);
    if (!reason?.trim()) return;
    try {
      if (approve) await correctionApi.approve(request.id, reason.trim());
      else await correctionApi.reject(request.id, reason.trim());
      setLoading(true);
      load();
    } catch { setError('Unable to process this correction request.'); }
  };

  return <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
    <header>
      <h1 style={{ fontSize: '1.5rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
        <ClipboardEdit size={24} color="#2563eb" /> Attendance Correction Requests
      </h1>
      <p style={{ color: 'var(--text-muted)' }}>{privileged ? 'Review pending employee requests.' : 'Track your submitted requests.'}</p>
    </header>
    {error && <div className="card" style={{ color: 'var(--danger)' }}>{error}</div>}
    <div className="card table-container">
      {loading ? <p>Loading requests...</p> : requests.length === 0 ? <p style={{ color: 'var(--text-muted)' }}>No correction requests found.</p> :
        <table><thead><tr><th>Employee</th><th>Requested times</th><th>Reason</th><th>Status</th>{privileged && <th>Actions</th>}</tr></thead>
          <tbody>{requests.map(request => <tr key={request.id}>
            <td>{request.requesterName}</td>
            <td>{new Date(request.proposedClockIn).toLocaleString()}<br />{request.proposedClockOut ? new Date(request.proposedClockOut).toLocaleString() : 'Open session'}</td>
            <td>{request.reason}</td><td><span className={`badge ${request.status === 'PENDING' ? 'badge-open' : request.status === 'APPROVED' ? 'badge-completed' : 'badge-inactive'}`}>{request.status}</span></td>
            {privileged && <td style={{ display: 'flex', gap: '0.5rem' }}><button className="btn btn-success" onClick={() => decide(request, true)}><Check size={15} />Approve</button><button className="btn btn-danger" onClick={() => decide(request, false)}><X size={15} />Reject</button></td>}
          </tr>)}</tbody>
        </table>}
    </div>
  </div>;
};
