import React, { useEffect, useRef, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { LogOut, User, Clock, Bell, CheckCheck } from 'lucide-react';
import { notificationApi } from '../services/api';
import { Notification } from '../types';
import { useNavigate } from 'react-router-dom';

export const Navbar: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unread, setUnread] = useState(0);
  const [open, setOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const loadNotifications = async () => {
    try {
      const [list, count] = await Promise.all([notificationApi.list(), notificationApi.unreadCount()]);
      setNotifications(list.data.content);
      setUnread(count.data);
    } catch { /* Notification availability must not block attendance UI. */ }
  };

  useEffect(() => {
    if (!user) return;
    loadNotifications();
    const timer = window.setInterval(loadNotifications, 30000);
    return () => window.clearInterval(timer);
  }, [user]);

  useEffect(() => {
    const close = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  const openNotification = async (notification: Notification) => {
    if (!notification.read) {
      await notificationApi.markRead(notification.id);
      setUnread(value => Math.max(0, value - 1));
      setNotifications(items => items.map(item => item.id === notification.id ? { ...item, read: true } : item));
    }
    setOpen(false);
    if (notification.link) navigate(notification.link);
  };

  return (
    <header style={{
      height: '64px',
      backgroundColor: '#ffffff',
      borderBottom: '1px solid #e2e8f0',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0 1.5rem',
      position: 'sticky',
      top: 0,
      zIndex: 40
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
        <div style={{
          backgroundColor: '#2563eb',
          color: 'white',
          padding: '0.5rem',
          borderRadius: '8px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center'
        }}>
          <Clock size={20} />
        </div>
        <span style={{ fontWeight: 700, fontSize: '1.125rem', color: '#0f172a' }}>
          Smart Attendance Platform
        </span>
      </div>

      {user && (
        <div style={{ display: 'flex', alignItems: 'center', gap: '1.25rem' }}>
          <div ref={dropdownRef} style={{ position: 'relative' }}>
            <button className="notification-button" onClick={() => setOpen(value => !value)} aria-label="Notifications">
              <Bell size={19} />
              {unread > 0 && <span className="notification-count">{unread > 9 ? '9+' : unread}</span>}
            </button>
            {open && <div className="notification-popover">
              <div className="notification-header"><strong>Notifications</strong><CheckCheck size={16} color="#64748b" /></div>
              {notifications.length === 0 ? <div className="notification-empty">You are all caught up.</div> :
                notifications.map(notification => <button key={notification.id} className={`notification-item ${notification.read ? '' : 'notification-unread'}`} onClick={() => openNotification(notification)}>
                  <strong>{notification.title}</strong><span>{notification.message}</span><small>{new Date(notification.createdAt).toLocaleString()}</small>
                </button>)}
            </div>}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.875rem' }}>
            <div style={{
              width: '32px',
              height: '32px',
              borderRadius: '50%',
              backgroundColor: '#e2e8f0',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#475569'
            }}>
              <User size={18} />
            </div>
            <div>
              <div style={{ fontWeight: 600 }}>{user.fullName}</div>
              <div style={{ fontSize: '0.75rem', color: '#64748b' }}>
                {user.role} · {user.employeeCode}
              </div>
            </div>
          </div>

          <button
            onClick={logout}
            className="btn btn-outline"
            style={{ padding: '0.4rem 0.8rem', fontSize: '0.8rem' }}
            title="Log Out"
          >
            <LogOut size={16} />
            <span>Logout</span>
          </button>
        </div>
      )}
    </header>
  );
};
