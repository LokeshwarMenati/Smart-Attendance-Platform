export type EmployeeRole = 'EMPLOYEE' | 'MANAGER' | 'ADMIN';
export type EmployeeStatus = 'ACTIVE' | 'INACTIVE';
export type EmployeeShift = 'MORNING' | 'EVENING' | 'NIGHT';
export type AttendanceStatus = 'OPEN' | 'COMPLETED';
export type CorrectionRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface Employee {
  id: string;
  employeeCode: string;
  firstName: string;
  lastName: string;
  fullName: string;
  email: string;
  role: EmployeeRole;
  status: EmployeeStatus;
  shift: EmployeeShift;
  createdAt: string;
  updatedAt: string;
}

export interface AttendanceRecord {
  id: string;
  employeeId: string;
  employeeCode: string;
  employeeName: string;
  attendanceDate: string;
  clockInTime: string;
  clockOutTime: string | null;
  durationMinutes: number | null;
  status: AttendanceStatus;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  isLast: boolean;
}

export interface LoginResponse {
  tokenType: string;
  accessToken: string;
  expiresInSeconds: number;
  employee: Employee;
}

export interface DailyReport {
  date: string;
  totalPresent: number;
  completedSessions: number;
  openSessions: number;
  totalDurationMinutes: number;
  averageDurationMinutes: number;
}

export interface AttendanceSummary {
  startDate: string;
  endDate: string;
  totalSessions: number;
  completedSessions: number;
  openSessions: number;
  totalDurationMinutes: number;
  averageDurationMinutes: number;
}

export interface CorrectionRequest {
  id: string;
  attendanceId: string;
  requesterId: string;
  requesterName: string;
  proposedClockIn: string;
  proposedClockOut: string | null;
  reason: string;
  status: CorrectionRequestStatus;
  reviewerName: string | null;
  decisionReason: string | null;
  reviewedAt: string | null;
  createdAt: string;
}

export interface Notification {
  id: string;
  title: string;
  message: string;
  link: string | null;
  type: string;
  read: boolean;
  createdAt: string;
}

export interface AttendanceAnalytics {
  date: string;
  totalEmployees: number;
  employeesPresent: number;
  employeesAbsent: number;
  lateArrivals: number;
  currentlyClockedIn: number;
  currentlyClockedOut: number;
  attendancePercentage: number;
  dailyTrend: { date: string; present: number; completed: number; open: number; totalMinutes: number }[];
  employeeHours: { employeeName: string; employeeCode: string; totalMinutes: number }[];
}
