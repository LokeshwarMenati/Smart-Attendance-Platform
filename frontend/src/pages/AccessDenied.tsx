import React from 'react';
import { Link } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';

export const AccessDenied: React.FC = () => {
  return (
    <div style={{ textAlign: 'center', padding: '4rem 2rem' }}>
      <div style={{
        display: 'inline-flex',
        padding: '1rem',
        borderRadius: '50%',
        backgroundColor: '#fee2e2',
        color: '#dc2626',
        marginBottom: '1rem'
      }}>
        <ShieldAlert size={40} />
      </div>
      <h1 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '0.5rem' }}>Access Forbidden</h1>
      <p style={{ color: '#64748b', marginBottom: '1.5rem', maxWidth: '400px', margin: '0 auto 1.5rem auto' }}>
        You do not have the required permissions (Role clearance) to access this administrative resource.
      </p>
      <Link to="/dashboard" className="btn btn-primary">
        Return to Dashboard
      </Link>
    </div>
  );
};

