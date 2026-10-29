import axios, { AxiosError } from 'axios';
import { AttendanceAnalytics, AttendanceRecord, AttendanceSummary, CorrectionRequest, DailyReport, Employee, LoginResponse, Notification, PageResponse } from '../types';

let inMemoryToken: string | null = sessionStorage.getItem('sat_auth_token');

export const setAuthToken = (token: string | null) => {
  inMemoryToken = token;
  if (token) {
    sessionStorage.setItem('sat_auth_token', token);
  } else {
    sessionStorage.removeItem('sat_auth_token');
  }
};

export const getAuthToken = () => inMemoryToken;

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json'
  }
});

// Request interceptor: attach Bearer token
api.interceptors.request.use(config => {
  const token = getAuthToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor: handle 401 unauthenticated
api.interceptors.response.use(
  response => response,
  (error: AxiosError) => {
    if (error.response && error.response.status === 401) {
      setAuthToken(null);
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export const authApi = {
  login: (credentials: { email: string; password: string }) =>
    api.post<LoginResponse>('/auth/login', credentials),
  me: () => api.get<Employee>('/auth/me')
};

export const attendanceApi = {
  clockIn: (notes?: string) =>
    api.post<AttendanceRecord>('/attendance/clock-in', { notes }),
  clockOut: (notes?: string) =>
    api.post<AttendanceRecord>('/attendance/clock-out', { notes }),
  getMyHistory: (params?: { startDate?: string; endDate?: string; page?: number; size?: number }) =>
    api.get<PageResponse<AttendanceRecord>>('/attendance/me', { params }),
  getEmployeeAttendance: (employeeId: string, params?: { startDate?: string; endDate?: string; page?: number; size?: number }) =>
    api.get<PageResponse<AttendanceRecord>>(`/attendance/employee/${employeeId}`, { params }),
  listAttendance: (params?: { date?: string; startDate?: string; endDate?: string; employeeId?: string; status?: string; search?: string; page?: number; size?: number }) =>
    api.get<PageResponse<AttendanceRecord>>('/attendance', { params })
  ,
  correct: (id: string, data: { clockInTime?: string; clockOutTime?: string; reason: string }) =>
    api.put<AttendanceRecord>(`/attendance/${id}`, data)
};

export const employeeApi = {
  list: (params?: { status?: string; search?: string; page?: number; size?: number }) =>
    api.get<PageResponse<Employee>>('/employees', { params }),
  getById: (id: string) =>
    api.get<Employee>(`/employees/${id}`),
  create: (data: { employeeCode: string; firstName: string; lastName: string; email: string; password: string; role: string }) =>
    api.post<Employee>('/employees', data),
  update: (id: string, data: { firstName: string; lastName: string; role: string; status: string }) =>
    api.put<Employee>(`/employees/${id}`, data),
  deactivate: (id: string) =>
    api.patch<Employee>(`/employees/${id}/deactivate`)
};

export const reportApi = {
  getAnalytics: () => api.get<AttendanceAnalytics>('/reports/attendance/analytics'),
  getDaily: (date?: string) =>
    api.get<DailyReport>('/reports/attendance/daily', { params: { date } }),
  getSummary: (params?: { startDate?: string; endDate?: string; employeeId?: string }) =>
    api.get<AttendanceSummary>('/reports/attendance/summary', { params }),
  downloadCsv: async (params?: { startDate?: string; endDate?: string; employeeId?: string }) => {
    const response = await api.get('/reports/attendance/export', {
      params,
      responseType: 'blob'
    });
    const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8;' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `attendance_export_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  }
};

export const correctionApi = {
  submit: (attendanceId: string, data: { proposedClockIn: string; proposedClockOut?: string; reason: string }) =>
    api.post<CorrectionRequest>(`/attendance/correction-requests/for/${attendanceId}`, data),
  list: (params?: { status?: string; page?: number; size?: number }) =>
    api.get<PageResponse<CorrectionRequest>>('/attendance/correction-requests', { params }),
  approve: (id: string, reason: string) =>
    api.post<CorrectionRequest>(`/attendance/correction-requests/${id}/approve`, { reason }),
  reject: (id: string, reason: string) =>
    api.post<CorrectionRequest>(`/attendance/correction-requests/${id}/reject`, { reason })
};

export const notificationApi = {
  list: () => api.get<PageResponse<Notification>>('/notifications', { params: { size: 10 } }),
  unreadCount: () => api.get<number>('/notifications/unread-count'),
  markRead: (id: string) => api.post(`/notifications/${id}/read`)
};

export default api;
