import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { XCircle, CheckCircle2, Globe, X } from 'lucide-react';
import { adminApi } from '../../api';
import { LoadingOverlay } from '../../components/Spinner';
import { Pagination } from '../../components/Pagination';
import { format } from 'date-fns';

export default function AdminLoginAttemptsPage() {
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState({ ip: '', success: '' });

  const { data, isLoading } = useQuery({
    queryKey: ['admin', 'login-attempts', page, filters],
    queryFn: () => adminApi.loginAttempts({
      page, size: 50,
      ...(filters.ip && { ip: filters.ip }),
      ...(filters.success !== '' && { success: filters.success === 'true' }),
    }).then(r => r.data),
    keepPreviousData: true,
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Globe size={22} className="text-primary-600" />
        <h1 className="page-title">Próby logowania</h1>
      </div>

      <div className="card p-4 grid grid-cols-1 md:grid-cols-4 gap-3">
        <div>
          <label className="label text-xs">Adres IP</label>
          <input value={filters.ip} onChange={e => { setFilters({ ...filters, ip: e.target.value }); setPage(0); }}
            className="input" placeholder="np. 192.168.1.1" />
        </div>
        <div>
          <label className="label text-xs">Status</label>
          <select value={filters.success} onChange={e => { setFilters({ ...filters, success: e.target.value }); setPage(0); }}
            className="input">
            <option value="">Wszystkie</option>
            <option value="true">Udane</option>
            <option value="false">Nieudane</option>
          </select>
        </div>
        <div className="md:col-span-2 flex items-end">
          {(filters.ip || filters.success !== '') && (
            <button onClick={() => { setFilters({ ip: '', success: '' }); setPage(0); }}
              className="btn-ghost text-gray-500"><X size={14} /> Wyczyść</button>
          )}
          <span className="ml-auto text-sm text-gray-500">
            Wyników: {data?.totalElements?.toLocaleString() ?? '—'}
          </span>
        </div>
      </div>

      <div className="card p-0 overflow-hidden">
        {isLoading && <LoadingOverlay />}
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr>
                <th className="table-header">Czas</th>
                <th className="table-header">Status</th>
                <th className="table-header">Użytkownik</th>
                <th className="table-header">IP</th>
                <th className="table-header">User-Agent</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {!isLoading && data?.content?.length === 0 ? (
                <tr><td colSpan={5} className="text-center py-12 text-gray-400">
                  <Globe size={36} className="mx-auto mb-2 opacity-30" />
                  <p className="text-sm">Brak prób logowania</p>
                </td></tr>
              ) : (
                data?.content?.map(a => (
                  <tr key={a.id} className="hover:bg-gray-50">
                    <td className="table-cell font-mono text-xs whitespace-nowrap">
                      {a.timestamp ? format(new Date(a.timestamp), 'yyyy-MM-dd HH:mm:ss') : '—'}
                    </td>
                    <td className="table-cell">
                      {a.success ? (
                        <span className="badge badge-green flex items-center gap-1 w-fit">
                          <CheckCircle2 size={12} /> OK
                        </span>
                      ) : (
                        <span className="badge badge-red flex items-center gap-1 w-fit">
                          <XCircle size={12} /> Fail
                        </span>
                      )}
                    </td>
                    <td className="table-cell text-sm">{a.username || <span className="text-gray-400">—</span>}</td>
                    <td className="table-cell font-mono text-xs">{a.ip || '—'}</td>
                    <td className="table-cell text-xs text-gray-500 max-w-sm truncate" title={a.userAgent}>
                      {a.userAgent || '—'}
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

