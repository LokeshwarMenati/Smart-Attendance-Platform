import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Clock, Lock, Mail, AlertCircle } from 'lucide-react';
import { AxiosError } from 'axios';

export const Login: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsSubmitting(true);

    try {
      await login(email, password);
      navigate('/dashboard');
    } catch (err) {
      const axiosErr = err as AxiosError<{ detail?: string; title?: string }>;
      const msg = axiosErr.response?.data?.detail || axiosErr.response?.data?.title || 'Login failed. Please check your credentials.';
      setError(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDemoFill = (demoEmail: string, demoPass: string) => {
    setEmail(demoEmail);
    setPassword(demoPass);
    setError(null);
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: '#f1f5f9',
      padding: '1.5rem'
    }}>
      <div className="card" style={{ maxWidth: '440px', width: '100%', padding: '2rem' }}>
        <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
          <div style={{
            display: 'inline-flex',
            backgroundColor: '#2563eb',
            color: 'white',
            padding: '0.75rem',
            borderRadius: '12px',
            marginBottom: '0.75rem'
          }}>
            <Clock size={28} />
          </div>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 700 }}>Smart Attendance</h1>
          <p style={{ color: '#64748b', fontSize: '0.875rem', marginTop: '0.25rem' }}>
            Enterprise Employee Attendance Platform
          </p>
        </div>

        {error && (
          <div style={{
            backgroundColor: '#fee2e2',
            border: '1px solid #f87171',
            color: '#b91c1c',
            padding: '0.75rem 1rem',
            borderRadius: '8px',
            marginBottom: '1rem',
            fontSize: '0.875rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem'
          }}>
            <AlertCircle size={18} />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 500, marginBottom: '0.375rem' }}>
              Work Email
            </label>
            <div style={{ position: 'relative' }}>
              <Mail size={18} style={{ position: 'absolute', left: '10px', top: '11px', color: '#94a3b8' }} />
              <input
                type="email"
                required
                className="input-field"
                style={{ paddingLeft: '2.25rem' }}
                placeholder="name@company.com"
                value={email}
                onChange={e => setEmail(e.target.value)}
              />
            </div>
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.875rem', fontWeight: 500, marginBottom: '0.375rem' }}>
              Password
            </label>
            <div style={{ position: 'relative' }}>
              <Lock size={18} style={{ position: 'absolute', left: '10px', top: '11px', color: '#94a3b8' }} />
              <input
                type="password"
                required
                className="input-field"
                style={{ paddingLeft: '2.25rem' }}
                placeholder="••••••••"
                value={password}
                onChange={e => setPassword(e.target.value)}
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="btn btn-primary"
            style={{ width: '100%', marginTop: '0.5rem' }}
          >
            {isSubmitting ? 'Authenticating...' : 'Sign In'}
          </button>
        </form>

        <div style={{ marginTop: '1.5rem', paddingTop: '1.25rem', borderTop: '1px solid #e2e8f0' }}>
          <div style={{ fontSize: '0.75rem', fontWeight: 600, color: '#64748b', marginBottom: '0.5rem', textAlign: 'center' }}>
            DEMO TEST ACCOUNTS
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '0.5rem' }}>
            <button
              type="button"
              onClick={() => handleDemoFill('admin@smartattendance.com', 'Admin@12345')}
              className="btn btn-outline"
              style={{ padding: '0.4rem', fontSize: '0.75rem' }}
            >
              Admin
            </button>
            <button
              type="button"
              onClick={() => handleDemoFill('manager@smartattendance.com', 'Manager@12345')}
              className="btn btn-outline"
              style={{ padding: '0.4rem', fontSize: '0.75rem' }}
            >
              Manager
            </button>
            <button
              type="button"
              onClick={() => handleDemoFill('employee@smartattendance.com', 'Employee@12345')}
              className="btn btn-outline"
              style={{ padding: '0.4rem', fontSize: '0.75rem' }}
            >
              Employee
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

