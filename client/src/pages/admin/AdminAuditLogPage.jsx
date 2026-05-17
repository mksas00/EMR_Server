import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { FileText, Search, X, User as UserIcon } from 'lucide-react';
import { adminApi } from '../../api';
import { LoadingOverlay } from '../../components/Spinner';
import { Pagination } from '../../components/Pagination';
import { format } from 'date-fns';

const ACTION_COLORS = {
  LOGIN_SUCCESS: 'badge-green',
  LOGIN_FAILURE: 'badge-red',
  LOGIN_LOCKED: 'badge-red',
  LOGIN_PWD_OK: 'badge-green',
  LOGIN_MFA_OK: 'badge-green',
  LOGIN_MFA_FAIL: 'badge-red',
  ACCOUNT_LOCK: 'badge-red',
  PASSWORD_CHANGE: 'badge-blue',
  PASSWORD_RESET_SUCCESS: 'badge-blue',
  PASSWORD_RESET_REQUEST: 'badge-yellow',
  REFRESH: 'badge-gray',
  LOGOUT: 'badge-gray',
  SESSION_REVOKE: 'badge-yellow',
  MFA_SETUP_CONFIRM: 'badge-green',
  MFA_DISABLE: 'badge-yellow',
  BTG_ACCESS: 'badge-red',
};

export default function AdminAuditLogPage() {
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState({ action: '', q: '', from: '', to: '' });
  const [draft, setDraft] = useState({ action: '', q: '', from: '', to: '' });

  const { data, isLoading } = useQuery({
    queryKey: ['admin', 'audit-log', page, filters],
    queryFn: () => adminApi.auditLog({
      page, size: 50,
      ...(filters.action && { action: filters.action }),
      ...(filters.q && { q: filters.q }),
      ...(filters.from && { from: new Date(filters.from).toISOString() }),
      ...(filters.to && { to: new Date(filters.to).toISOString() }),
    }).then(r => r.data),
    keepPreviousData: true,
  });

  const apply = e => {
    e.preventDefault();
    setFilters(draft);
    setPage(0);
  };

  const clear = () => {
    setDraft({ action: '', q: '', from: '', to: '' });
    setFilters({ action: '', q: '', from: '', to: '' });
    setPage(0);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <FileText size={22} className="text-primary-600" />
        <h1 className="page-title">Dziennik audytu</h1>
      </div>

      {/* Filters */}
      <form onSubmit={apply} className="card p-4 grid grid-cols-1 md:grid-cols-5 gap-3">
        <div className="md:col-span-2">
          <label className="label text-xs">Szukaj w opisie</label>
          <div className="relative">
            <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input value={draft.q} onChange={e => setDraft({ ...draft, q: e.target.value })}
              className="input pl-9" placeholder="np. logowanie..." />
          </div>
        </div>
        <div>
          <label className="label text-xs">Akcja</label>
          <input value={draft.action} onChange={e => setDraft({ ...draft, action: e.target.value })}
            className="input" placeholder="LOGIN_SUCCESS..." />
        </div>
        <div>
          <label className="label text-xs">Od</label>
          <input type="datetime-local" value={draft.from}
            onChange={e => setDraft({ ...draft, from: e.target.value })} className="input" />
        </div>
        <div>
          <label className="label text-xs">Do</label>
          <input type="datetime-local" value={draft.to}
            onChange={e => setDraft({ ...draft, to: e.target.value })} className="input" />
        </div>
        <div className="md:col-span-5 flex gap-2">
          <button type="submit" className="btn-primary"><Search size={14} /> Filtruj</button>
          {(filters.action || filters.q || filters.from || filters.to) && (
            <button type="button" onClick={clear} className="btn-ghost text-gray-500">
              <X size={14} /> Wyczyść
            </button>
          )}
          <span className="ml-auto text-sm text-gray-500 self-center">
            Wyników: {data?.totalElements?.toLocaleString() ?? '—'}
          </span>
        </div>
      </form>

      {/* Table */}
      <div className="card p-0 overflow-hidden">
        {isLoading && <LoadingOverlay />}
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr>
                <th className="table-header">Czas</th>
                <th className="table-header">Akcja</th>
                <th className="table-header">Użytkownik</th>
                <th className="table-header">Opis</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {!isLoading && data?.content?.length === 0 ? (
                <tr><td colSpan={4} className="text-center py-12 text-gray-400">
                  <FileText size={36} className="mx-auto mb-2 opacity-30" />
                  <p className="text-sm">Brak wpisów</p>
                </td></tr>
              ) : (
                data?.content?.map(row => (
                  <tr key={row.id} className="hover:bg-gray-50">
                    <td className="table-cell font-mono text-xs whitespace-nowrap">
                      {row.timestamp ? format(new Date(row.timestamp), 'yyyy-MM-dd HH:mm:ss') : '—'}
                    </td>
                    <td className="table-cell">
                      <span className={`badge ${ACTION_COLORS[row.action] || 'badge-gray'} text-xs font-mono`}>
                        {row.action || '—'}
                      </span>
                    </td>
                    <td className="table-cell text-sm">
                      {row.username ? (
                        <span className="flex items-center gap-1 text-gray-700">
                          <UserIcon size={12} className="text-gray-400" /> {row.username}
                        </span>
                      ) : <span className="text-gray-400">—</span>}
                    </td>
                    <td className="table-cell text-sm text-gray-600 max-w-2xl">
                      <p className="line-clamp-2">{row.description || '—'}</p>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        <Pagination page={page} totalPages={data?.totalPages || 0} onPageChange={setPage} />
      </div>
    </div>
  );
}

