import React from 'react';
import { Link } from 'react-router-dom';
import { FileQuestion } from 'lucide-react';

export const NotFound: React.FC = () => {
  return (
    <div style={{ textAlign: 'center', padding: '4rem 2rem' }}>
      <div style={{
        display: 'inline-flex',
        padding: '1rem',
        borderRadius: '50%',
        backgroundColor: '#f1f5f9',
        color: '#64748b',
        marginBottom: '1rem'
      }}>
        <FileQuestion size={40} />
      </div>
      <h1 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '0.5rem' }}>404 - Page Not Found</h1>
      <p style={{ color: '#64748b', marginBottom: '1.5rem', maxWidth: '400px', margin: '0 auto 1.5rem auto' }}>
        The page you are attempting to reach does not exist or has been relocated.
      </p>
      <Link to="/dashboard" className="btn btn-primary">
        Return to Dashboard
      </Link>
    </div>
  );
};

