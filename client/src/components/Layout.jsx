import { Fragment, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Dialog, Transition } from '@headlessui/react';
import { useQuery } from '@tanstack/react-query';
import {
  LayoutDashboard,
  Users,
  Calendar,
  FolderOpen,
  Shield,
  Menu,
  X,
  LogOut,
  User,
  Stethoscope,
  Pill,
  FlaskConical,
  Lock,
} from 'lucide-react';

import { useAuth } from '../contexts/AuthContext';
import { adminApi } from '../api';

const navigation = [
  { name: 'Dashboard', href: '/', icon: LayoutDashboard },
  { name: 'Pacjenci', href: '/patients', icon: Users },
  { name: 'Wizyty', href: '/visits', icon: Calendar },
  { name: 'Zlecenia lab.', href: '/lab-orders', icon: FlaskConical },
  { name: 'Moje dostępy', href: '/consents/mine', icon: Lock },
  { name: 'Leki', href: '/medications', icon: Pill },
  { name: 'Profil', href: '/profile', icon: User },
];

const adminNavigation = [
  { name: 'Dashboard', href: '/admin', icon: Shield },
  { name: 'Audyt', href: '/admin/audit-log', icon: FolderOpen },
  { name: 'Incydenty', href: '/admin/incidents', icon: Shield },
  { name: 'Próby logowania', href: '/admin/login-attempts', icon: Shield },
  { name: 'Sesje', href: '/admin/sessions', icon: Shield },
  { name: 'Użytkownicy', href: '/admin/users', icon: Users },
];

function NavItem({ item, mobile = false, onClick, badge }) {
  const location = useLocation();
  const isActive = location.pathname === item.href ||
    (item.href !== '/' && location.pathname.startsWith(item.href));
  const Icon = item.icon;

  const cls = mobile
    ? `flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${isActive ? 'bg-primary-700 text-white' : 'text-primary-100 hover:bg-primary-700 hover:text-white'}`
    : `flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${isActive ? 'bg-primary-700 text-white' : 'text-primary-100 hover:bg-primary-700 hover:text-white'}`;

  return (
    <Link to={item.href} className={cls} onClick={onClick}>
      <Icon size={18} />
      <span className="flex-1">{item.name}</span>
      {badge != null && badge > 0 && (
        <span className="inline-flex items-center justify-center min-w-[20px] h-5 px-1.5 text-[10px] font-bold rounded-full bg-red-500 text-white animate-pulse">
          {badge > 99 ? '99+' : badge}
        </span>
      )}
    </Link>
  );
}

export function Layout({ children }) {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const { user, logout, isAdmin } = useAuth();
  const navigate = useNavigate();

  // Liczba otwartych krytycznych+wysokich incydentów (badge w sidebarze)
  const { data: criticalIncidents } = useQuery({
    queryKey: ['admin', 'incidents-badge'],
    queryFn: () => adminApi.incidents({ status: 'open', size: 100, page: 0 }).then(r => r.data),
    enabled: isAdmin,
    refetchInterval: 60_000,
  });
  const criticalCount = (criticalIncidents?.content || [])
    .filter(i => i.severity === 'critical' || i.severity === 'high').length;

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const roleLabel = {
    admin: 'Administrator',
    doctor: 'Lekarz',
    nurse: 'Pielęgniarka',
    patient: 'Pacjent',
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Mobile sidebar */}
      <Transition.Root show={sidebarOpen} as={Fragment}>
        <Dialog as="div" className="relative z-50 lg:hidden" onClose={setSidebarOpen}>
          <Transition.Child
            as={Fragment}
            enter="transition-opacity ease-linear duration-300"
            enterFrom="opacity-0"
            enterTo="opacity-100"
            leave="transition-opacity ease-linear duration-300"
            leaveFrom="opacity-100"
            leaveTo="opacity-0"
          >
            <div className="fixed inset-0 bg-gray-900/80" />
          </Transition.Child>
          <div className="fixed inset-0 flex">
            <Transition.Child
              as={Fragment}
              enter="transition ease-in-out duration-300 transform"
              enterFrom="-translate-x-full"
              enterTo="translate-x-0"
              leave="transition ease-in-out duration-300 transform"
              leaveFrom="translate-x-0"
              leaveTo="-translate-x-full"
            >
              <Dialog.Panel className="relative mr-16 flex w-full max-w-xs flex-1">
                <div className="flex grow flex-col gap-y-5 overflow-y-auto bg-primary-800 px-6 pb-4">
                  <div className="flex h-16 shrink-0 items-center justify-between">
                    <div className="flex items-center gap-2 text-white font-bold text-lg">
                      <Stethoscope size={24} />
                      EMR System
                    </div>
                    <button onClick={() => setSidebarOpen(false)} className="text-white">
                      <X size={20} />
                    </button>
                  </div>
                  <nav className="flex flex-col gap-1">
                    {navigation.map((item) => (
                      <NavItem key={item.name} item={item} mobile onClick={() => setSidebarOpen(false)} />
                    ))}
                    {isAdmin && (
                      <>
                        <p className="text-xs text-primary-300 uppercase tracking-wider mt-4 mb-1 px-3">Administracja</p>
                        {adminNavigation.map((item) => (
                          <NavItem
                            key={item.name}
                            item={item}
                            mobile
                            onClick={() => setSidebarOpen(false)}
                            badge={item.href === '/admin/incidents' || item.href === '/admin' ? criticalCount : undefined}
                          />
                        ))}
                      </>
                    )}
                  </nav>
                </div>
              </Dialog.Panel>
            </Transition.Child>
          </div>
        </Dialog>
      </Transition.Root>

      {/* Desktop sidebar */}
      <div className="hidden lg:fixed lg:inset-y-0 lg:z-50 lg:flex lg:w-64 lg:flex-col">
        <div className="flex grow flex-col gap-y-5 overflow-y-auto bg-primary-800 px-6 pb-4">
          <div className="flex h-16 shrink-0 items-center gap-2 text-white font-bold text-xl">
            <Stethoscope size={26} />
            EMR System
          </div>
          <nav className="flex flex-1 flex-col gap-1">
            {navigation.map((item) => (
              <NavItem key={item.name} item={item} />
            ))}
            {isAdmin && (
              <>
                <p className="text-xs text-primary-300 uppercase tracking-wider mt-4 mb-1 px-3">Administracja</p>
                {adminNavigation.map((item) => (
                  <NavItem
                    key={item.name}
                    item={item}
                    badge={item.href === '/admin/incidents' || item.href === '/admin' ? criticalCount : undefined}
                  />
                ))}
              </>
            )}
          </nav>
          {/* User info */}
          <div className="border-t border-primary-700 pt-4">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-full bg-primary-600 flex items-center justify-center text-white text-sm font-semibold">
                {user?.username?.[0]?.toUpperCase() || 'U'}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-white truncate">{user?.username}</p>
                <p className="text-xs text-primary-300 capitalize">
                  {roleLabel[user?.role?.toLowerCase()] || user?.role}
                </p>
              </div>
              <button
                onClick={handleLogout}
                className="text-primary-300 hover:text-white transition-colors"
                title="Wyloguj"
              >
                <LogOut size={18} />
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Main content */}
      <div className="lg:pl-64">
        {/* Topbar */}
        <div className="sticky top-0 z-40 flex h-14 items-center gap-4 bg-white border-b border-gray-200 px-4 shadow-sm">
          <button onClick={() => setSidebarOpen(true)} className="lg:hidden text-gray-600">
            <Menu size={22} />
          </button>
          <div className="flex-1" />
          <div className="flex items-center gap-3 text-sm text-gray-600">
            <span className="hidden sm:block">
              {user?.username} ({roleLabel[user?.role?.toLowerCase()] || user?.role})
            </span>
            <button
              onClick={handleLogout}
              className="flex items-center gap-1 text-gray-500 hover:text-red-600 transition-colors"
            >
              <LogOut size={16} />
              <span className="hidden sm:block">Wyloguj</span>
            </button>
          </div>
        </div>

        {/* Page content */}
        <main className="p-6">
          {children}
        </main>
      </div>
    </div>
  );
}

