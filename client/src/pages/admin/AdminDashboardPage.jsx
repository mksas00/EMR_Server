import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Shield, AlertTriangle, Activity, Lock, Users as UsersIcon,
  XCircle, CheckCircle2, Globe, UserCog, FileText, ArrowRight,
} from 'lucide-react';
import { adminApi } from '../../api';
import { LoadingOverlay } from '../../components/Spinner';

const SEVERITY_COLORS = {
  low: 'bg-blue-100 text-blue-700 border-blue-200',
  medium: 'bg-yellow-100 text-yellow-700 border-yellow-200',
  high: 'bg-orange-100 text-orange-700 border-orange-200',
  critical: 'bg-red-100 text-red-700 border-red-200',
};

function StatCard({ icon: Icon, label, value, hint, color = 'primary', to }) {
  const colors = {
    primary: 'bg-primary-50 text-primary-700 border-primary-100',
    red: 'bg-red-50 text-red-700 border-red-100',
    green: 'bg-emerald-50 text-emerald-700 border-emerald-100',
    yellow: 'bg-yellow-50 text-yellow-700 border-yellow-100',
    gray: 'bg-gray-50 text-gray-700 border-gray-100',
  };
  const Wrapper = to ? Link : 'div';
  return (
    <Wrapper to={to} className={`card p-5 border ${colors[color]} ${to ? 'hover:shadow-md transition-shadow cursor-pointer' : ''}`}>
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs uppercase tracking-wider text-gray-500 font-medium">{label}</p>
          <p className="text-3xl font-bold mt-1">{value?.toLocaleString() ?? '—'}</p>
          {hint && <p className="text-xs text-gray-500 mt-1">{hint}</p>}
        </div>
        <Icon size={28} className="opacity-60" />
      </div>
    </Wrapper>
  );
}

export default function AdminDashboardPage() {
  const [windowDays, setWindowDays] = useState(7);
  const { data, isLoading } = useQuery({
    queryKey: ['admin', 'dashboard', windowDays],
    queryFn: () => adminApi.dashboard({ windowDays }).then(r => r.data),
    refetchInterval: 30_000,
  });

  // Otwarte krytyczne / wysokie incydenty - zawsze 30s odświeżanie
  const { data: criticalData } = useQuery({
    queryKey: ['admin', 'incidents', 'open-critical'],
    queryFn: () => adminApi.incidents({ status: 'open', size: 5, page: 0 }).then(r => r.data),
    refetchInterval: 30_000,
  });
  const criticalIncidents = (criticalData?.content || [])
    .filter(i => i.severity === 'critical' || i.severity === 'high')
    .slice(0, 5);

  if (isLoading) return <LoadingOverlay />;

  const s = data?.stats || {};
  const severities = data?.incidentsBySeverity || [];
  const topIps = data?.topFailingIps || [];
  const topUsers = data?.topActiveUsers || [];

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Shield size={24} className="text-primary-600" />
        <h1 className="page-title">Bezpieczeństwo systemu</h1>
        <div className="ml-auto flex items-center gap-2">
          <label className="text-xs text-gray-500">Okno:</label>
          <select
            value={windowDays}
            onChange={(e) => setWindowDays(Number(e.target.value))}
            className="input py-1 text-xs w-auto"
          >
            <option value={1}>24 godziny</option>
            <option value={7}>7 dni</option>
            <option value={30}>30 dni</option>
            <option value={90}>90 dni</option>
            <option value={365}>1 rok</option>
          </select>
          <span className="badge badge-blue">odświeża co 30s</span>
        </div>
      </div>

      {/* Critical / high open incidents banner */}
      {criticalIncidents.length > 0 && (
        <div className="rounded-lg border-2 border-red-300 bg-red-50 p-4">
          <div className="flex items-center gap-2 mb-3">
            <AlertTriangle size={20} className="text-red-600 animate-pulse" />
            <h2 className="font-semibold text-red-900">
              Otwarte incydenty wymagające uwagi ({criticalIncidents.length})
            </h2>
            <Link to="/admin/incidents" className="ml-auto text-xs text-red-700 hover:underline flex items-center gap-1">
              Zarządzaj <ArrowRight size={12} />
            </Link>
          </div>
          <ul className="space-y-2">
            {criticalIncidents.map(inc => (
              <li key={inc.id} className="flex items-center gap-3 text-sm bg-white rounded p-2 border border-red-100">
                <span className={`badge ${SEVERITY_COLORS[inc.severity] || 'badge-gray'} capitalize text-xs w-20 justify-center`}>
                  {inc.severity}
                </span>
                <span className="font-mono text-xs text-gray-500 w-44 truncate">{inc.category}</span>
                <span className="flex-1 text-gray-700 truncate">{inc.description}</span>
                <span className="text-xs text-gray-400 whitespace-nowrap">
                  {new Date(inc.detectedAt).toLocaleString('pl-PL', { dateStyle: 'short', timeStyle: 'short' })}
                </span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Stats grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
        <StatCard
          icon={Activity} label="Aktywne sesje" value={s.activeSessions}
          color="primary" to="/admin/sessions"
        />
        <StatCard
          icon={AlertTriangle} label="Otwarte incydenty" value={s.openIncidents}
          color={s.openIncidents > 0 ? 'red' : 'green'} to="/admin/incidents"
        />
        <StatCard
          icon={XCircle} label="Nieudane logowania (24h)" value={s.failedLogins24h}
          hint={s.failedLogins24h > 10 ? 'Podwyższona aktywność' : 'W normie'}
          color={s.failedLogins24h > 10 ? 'yellow' : 'gray'}
          to="/admin/login-attempts"
        />
        <StatCard
          icon={CheckCircle2} label="Udane logowania (24h)" value={s.successfulLogins24h}
          color="green"
        />
        <StatCard
          icon={Lock} label="Zablokowane konta" value={s.lockedAccounts}
          color={s.lockedAccounts > 0 ? 'yellow' : 'gray'}
          to="/admin/users?locked=true"
        />
        <StatCard
          icon={UsersIcon} label="Wszyscy użytkownicy" value={s.totalUsers}
          color="primary" to="/admin/users"
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Incidents by severity */}
        <div className="card p-5">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-800 flex items-center gap-2">
              <AlertTriangle size={18} className="text-orange-500" />
              Incydenty wg ważności (30 dni)
            </h2>
            <Link to="/admin/incidents" className="text-xs text-primary-600 hover:underline flex items-center gap-1">
              Pokaż wszystkie <ArrowRight size={12} />
            </Link>
          </div>
          {severities.every(x => x.count === 0) ? (
            <p className="text-sm text-gray-400 text-center py-8">Brak incydentów w ostatnich 30 dniach 🎉</p>
          ) : (
            <div className="space-y-2">
              {severities.map(sv => (
                <div key={sv.severity} className="flex items-center gap-3">
                  <span className={`badge border ${SEVERITY_COLORS[sv.severity] || 'badge-gray'} w-24 justify-center capitalize`}>
                    {sv.severity}
                  </span>
                  <div className="flex-1 bg-gray-100 rounded-full h-2.5 overflow-hidden">
                    <div
                      className="h-full bg-current opacity-50"
                      style={{
                        width: `${Math.min(100, (sv.count / Math.max(1, Math.max(...severities.map(x => x.count)))) * 100)}%`,
                      }}
                    />
                  </div>
                  <span className="text-sm font-mono text-gray-700 w-8 text-right">{sv.count}</span>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Top failing IPs */}
        <div className="card p-5">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-800 flex items-center gap-2">
              <Globe size={18} className="text-red-500" />
              IP z największą liczbą porażek ({windowDays === 1 ? '24h' : `${windowDays} dni`})
            </h2>
          </div>
          {topIps.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-8">Brak prób z porażkami</p>
          ) : (
            <table className="w-full text-sm">
              <tbody>
                {topIps.map(item => (
                  <tr key={item.key} className="border-b border-gray-50 last:border-0">
                    <td className="py-2 font-mono text-gray-700">{item.key}</td>
                    <td className="py-2 text-right">
                      <span className="badge badge-red">{item.count}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Top users */}
        <div className="card p-5">
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-gray-800 flex items-center gap-2">
              <UserCog size={18} className="text-primary-500" />
              Najaktywniejsi użytkownicy ({windowDays === 1 ? '24h' : `${windowDays} dni`})
            </h2>
          </div>
          {topUsers.length === 0 ? (
            <p className="text-sm text-gray-400 text-center py-8">Brak aktywności</p>
          ) : (
            <table className="w-full text-sm">
              <tbody>
                {topUsers.map(item => (
                  <tr key={item.key} className="border-b border-gray-50 last:border-0">
                    <td className="py-2 text-gray-700">{item.key}</td>
                    <td className="py-2 text-right">
                      <span className="badge badge-blue">{item.count} logowań</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Quick links */}
        <div className="card p-5">
          <h2 className="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
            <FileText size={18} className="text-gray-500" />
            Szybki dostęp
          </h2>
          <div className="grid grid-cols-2 gap-3">
            <Link to="/admin/audit-log" className="btn-secondary text-sm justify-start">
              <FileText size={14} /> Dziennik audytu
            </Link>
            <Link to="/admin/login-attempts" className="btn-secondary text-sm justify-start">
              <XCircle size={14} /> Próby logowania
            </Link>
            <Link to="/admin/incidents" className="btn-secondary text-sm justify-start">
              <AlertTriangle size={14} /> Incydenty
            </Link>
            <Link to="/admin/sessions" className="btn-secondary text-sm justify-start">
              <Activity size={14} /> Aktywne sesje
            </Link>
            <Link to="/admin/users" className="btn-secondary text-sm justify-start col-span-2">
              <UsersIcon size={14} /> Zarządzanie kontami
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}

