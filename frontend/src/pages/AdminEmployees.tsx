import React, { useState, useEffect, useCallback } from 'react';
import { employeeApi } from '../services/api';
import { Employee, EmployeeRole } from '../types';
import { ShieldCheck, UserPlus, Search, UserX, ChevronLeft, ChevronRight } from 'lucide-react';
import { ConfirmationModal } from '../components/ConfirmationModal';
import { AxiosError } from 'axios';
import { useAuth } from '../context/AuthContext';

export const AdminEmployees: React.FC = () => {
  const { user } = useAuth();
  const canAdminister = user?.role === 'ADMIN';
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(false);

  // Modal form state
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [code, setCode] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<EmployeeRole>('EMPLOYEE');
  const [formError, setFormError] = useState<string | null>(null);

  // Deactivate state
  const [deactivatingEmployee, setDeactivatingEmployee] = useState<Employee | null>(null);
  const [selectedEmployee, setSelectedEmployee] = useState<Employee | null>(null);

  const fetchEmployees = useCallback(async () => {
    setLoading(true);
    try {
      const res = await employeeApi.list({
        page,
        size: 15,
        search: search || undefined
      });
      setEmployees(res.data.content);
      setTotalPages(res.data.totalPages);
    } catch (err) {
      console.error('Failed to load employees', err);
    } finally {
      setLoading(false);
    }
  }, [page, search]);

  useEffect(() => {
    fetchEmployees();
  }, [fetchEmployees]);

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    try {
      await employeeApi.create({
        employeeCode: code,
        firstName,
        lastName,
        email,
        password,
        role
      });
      setIsCreateOpen(false);
      setCode('');
      setFirstName('');
      setLastName('');
      setEmail('');
      setPassword('');
      fetchEmployees();
    } catch (err) {
      const axiosErr = err as AxiosError<{ detail?: string }>;
      setFormError(axiosErr.response?.data?.detail || 'Failed to create employee.');
    }
  };

  const handleDeactivate = async () => {
    if (!deactivatingEmployee) return;
    try {
      await employeeApi.deactivate(deactivatingEmployee.id);
      setDeactivatingEmployee(null);
      fetchEmployees();
    } catch (err) {
      console.error('Failed to deactivate employee', err);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#0f172a', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <ShieldCheck size={24} color="#2563eb" />
            <span>Employee Directory & Administration</span>
          </h1>
          <p style={{ color: '#64748b', fontSize: '0.875rem' }}>
            Register new team members, manage roles, and enforce account statuses.
          </p>
        </div>

        {canAdminister && <button
          onClick={() => setIsCreateOpen(true)}
          className="btn btn-primary"
        >
          <UserPlus size={18} />
          <span>Add Employee</span>
        </button>}
      </div>

      {/* Search Bar */}
      <div className="card" style={{ padding: '0.75rem 1.25rem' }}>
        <div style={{ position: 'relative', maxWidth: '380px' }}>
          <Search size={16} style={{ position: 'absolute', left: '10px', top: '12px', color: '#94a3b8' }} />
          <input
            type="text"
            className="input-field"
            style={{ paddingLeft: '2.25rem' }}
            placeholder="Search by name, email, or code..."
            value={search}
            onChange={e => { setSearch(e.target.value); setPage(0); }}
          />
        </div>
      </div>

      {/* Table */}
      <div className="card">
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Code</th>
                <th>Full Name</th>
                <th>Email</th>
                <th>Shift</th>
                <th>Role</th>
                <th>Status</th>
                <th>Joined</th>
                <th style={{ textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={8} style={{ textAlign: 'center', color: '#64748b', padding: '2rem' }}>
                    Loading directory...
                  </td>
                </tr>
              ) : employees.length === 0 ? (
                <tr>
                  <td colSpan={8} style={{ textAlign: 'center', color: '#64748b', padding: '2rem' }}>
                    No employees found matching query.
                  </td>
                </tr>
              ) : (
                employees.map(emp => (
                  <tr key={emp.id}>
                    <td><strong style={{ fontFamily: 'monospace' }}>{emp.employeeCode}</strong></td>
                    <td><button className="employee-link" onClick={() => setSelectedEmployee(emp)}>{emp.fullName}</button></td>
                    <td>{emp.email}</td>
                    <td><span className="badge badge-active">{emp.shift}</span></td>
                    <td><span className="badge badge-active">{emp.role}</span></td>
                    <td>
                      <span className={`badge ${emp.status === 'ACTIVE' ? 'badge-completed' : 'badge-inactive'}`}>
                        {emp.status}
                      </span>
                    </td>
                    <td>{new Date(emp.createdAt).toLocaleDateString()}</td>
                    <td style={{ textAlign: 'right' }}>
                      {!canAdminister && <button className="btn btn-outline" style={{ padding: '0.3rem 0.6rem', marginRight: '0.5rem' }} onClick={() => setSelectedEmployee(emp)}>Details</button>}
                      {canAdminister && emp.status === 'ACTIVE' && (
                        <button
                          onClick={() => setDeactivatingEmployee(emp)}
                          className="btn btn-outline"
                          style={{ padding: '0.3rem 0.6rem', color: '#dc2626' }}
                          title="Deactivate account"
                        >
                          <UserX size={15} />
                          <span>Deactivate</span>
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {selectedEmployee && (
          <div className="modal-backdrop" onClick={() => setSelectedEmployee(null)}>
            <div className="modal-card" onClick={event => event.stopPropagation()}>
              <h2>Employee details</h2>
              <div className="employee-detail-grid">
                <span>Employee ID</span><strong>{selectedEmployee.employeeCode}</strong>
                <span>Name</span><strong>{selectedEmployee.fullName}</strong>
                <span>Email</span><strong>{selectedEmployee.email}</strong>
                <span>Shift</span><strong>{selectedEmployee.shift} ({selectedEmployee.shift === 'MORNING' ? '06:00–14:00' : selectedEmployee.shift === 'EVENING' ? '14:00–22:00' : '22:00–06:00'})</strong>
                <span>Role</span><strong>{selectedEmployee.role}</strong>
                <span>Status</span><strong>{selectedEmployee.status}</strong>
              </div>
              <button className="btn btn-outline" onClick={() => setSelectedEmployee(null)}>Close</button>
            </div>
          </div>
        )}

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

      {/* Create Employee Modal */}
      {isCreateOpen && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 100
        }}>
          <div style={{
            backgroundColor: '#ffffff',
            borderRadius: '12px',
            padding: '1.75rem',
            maxWidth: '500px',
            width: '90%',
            boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1)'
          }}>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1rem' }}>
              Register New Employee
            </h3>

            {formError && (
              <div style={{ backgroundColor: '#fee2e2', border: '1px solid #f87171', color: '#b91c1c', padding: '0.5rem 0.75rem', borderRadius: '6px', marginBottom: '1rem', fontSize: '0.8rem' }}>
                {formError}
              </div>
            )}

            <form onSubmit={handleCreateSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 500, marginBottom: '0.25rem' }}>Code *</label>
                  <input
                    type="text"
                    required
                    placeholder="EMP-099"
                    className="input-field"
                    value={code}
                    onChange={e => setCode(e.target.value)}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 500, marginBottom: '0.25rem' }}>Role *</label>
                  <select
                    className="input-field"
                    value={role}
                    onChange={e => setRole(e.target.value as EmployeeRole)}
                  >
                    <option value="EMPLOYEE">EMPLOYEE</option>
                    <option value="MANAGER">MANAGER</option>
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 500, marginBottom: '0.25rem' }}>First Name *</label>
                  <input
                    type="text"
                    required
                    className="input-field"
                    value={firstName}
                    onChange={e => setFirstName(e.target.value)}
                  />
                </div>
                <div>
                  <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 500, marginBottom: '0.25rem' }}>Last Name *</label>
                  <input
                    type="text"
                    required
                    className="input-field"
                    value={lastName}
                    onChange={e => setLastName(e.target.value)}
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 500, marginBottom: '0.25rem' }}>Email *</label>
                <input
                  type="email"
                  required
                  className="input-field"
                  placeholder="alex@company.com"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 500, marginBottom: '0.25rem' }}>Initial Password *</label>
                <input
                  type="password"
                  required
                  placeholder="Min 8 characters"
                  className="input-field"
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                <button type="button" onClick={() => setIsCreateOpen(false)} className="btn btn-outline">
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Create Account
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Deactivate Modal */}
      <ConfirmationModal
        isOpen={!!deactivatingEmployee}
        title="Confirm Deactivation"
        message={`Are you sure you want to deactivate ${deactivatingEmployee?.fullName} (${deactivatingEmployee?.employeeCode})? Inactive employees cannot log in or record attendance.`}
        confirmLabel="Deactivate Employee"
        isDanger={true}
        onConfirm={handleDeactivate}
        onCancel={() => setDeactivatingEmployee(null)}
      />
    </div>
  );
};
