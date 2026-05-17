import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Activity, X, Trash2 } from 'lucide-react';
import { toast } from 'react-toastify';
import { adminApi } from '../../api';
import { LoadingOverlay } from '../../components/Spinner';
import { Pagination } from '../../components/Pagination';
import { ConfirmModal } from '../../components/Modal';
import { format } from 'date-fns';

export default function AdminSessionsPage() {
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const [activeOnly, setActiveOnly] = useState(true);
  const [revokeTarget, setRevokeTarget] = useState(null);

  const { data, isLoading } = useQuery({
    queryKey: ['admin', 'sessions', page, activeOnly],
    queryFn: () => adminApi.sessions({ page, size: 50, activeOnly }).then(r => r.data),
    keepPreviousData: true,
  });

  const revoke = useMutation({
    mutationFn: id => adminApi.revokeSession(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin', 'sessions'] });
      qc.invalidateQueries({ queryKey: ['admin', 'dashboard'] });
      setRevokeTarget(null);
      toast.success('Sesja unieważniona');
    },
    onError: e => toast.error(e.response?.data?.message || 'Błąd unieważniania'),
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Activity size={22} className="text-primary-600" />
        <h1 className="page-title">Aktywne sesje (wszyscy użytkownicy)</h1>
      </div>

      <div className="card p-4 flex items-center gap-3">
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={activeOnly}
            onChange={e => { setActiveOnly(e.target.checked); setPage(0); }}
            className="rounded border-gray-300" />
          Tylko aktywne
        </label>
        <span className="ml-auto text-sm text-gray-500">
          Wyników: {data?.totalElements?.toLocaleString() ?? '—'}
        </span>
      </div>

      <div className="card p-0 overflow-hidden">
        {isLoading && <LoadingOverlay />}
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr>
                <th className="table-header">Użytkownik</th>
                <th className="table-header">Rola</th>
                <th className="table-header">IP</th>
                <th className="table-header">Klient</th>
                <th className="table-header">Wydana</th>
                <th className="table-header">Wygasa</th>
                <th className="table-header">Status</th>
                <th className="table-header text-right">Akcje</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {!isLoading && data?.content?.length === 0 ? (
                <tr><td colSpan={8} className="text-center py-12 text-gray-400">
                  <Activity size={36} className="mx-auto mb-2 opacity-30" />
                  <p className="text-sm">Brak sesji</p>
                </td></tr>
              ) : (
                data?.content?.map(s => (
                  <tr key={s.id} className="hover:bg-gray-50">
                    <td className="table-cell text-sm font-medium">{s.username || '—'}</td>
                    <td className="table-cell">
                      <span className="badge badge-blue text-xs capitalize">{s.role || '—'}</span>
                    </td>
                    <td className="table-cell font-mono text-xs">{s.ip || '—'}</td>
                    <td className="table-cell text-xs text-gray-500 max-w-xs truncate" title={s.userAgent}>
                      {s.userAgent || '—'}
                    </td>
                    <td className="table-cell font-mono text-xs">
                      {s.issuedAt ? format(new Date(s.issuedAt), 'yyyy-MM-dd HH:mm') : '—'}
                    </td>
                    <td className="table-cell font-mono text-xs">
                      {s.expiresAt ? format(new Date(s.expiresAt), 'yyyy-MM-dd HH:mm') : '—'}
                    </td>
                    <td className="table-cell">
                      <span className={`badge ${s.active ? 'badge-green' : 'badge-gray'}`}>
                        {s.active ? 'Aktywna' : (s.revokedAt ? 'Unieważniona' : 'Wygasła')}
                      </span>
                    </td>
                    <td className="table-cell text-right">
                      {s.active && (
                        <button onClick={() => setRevokeTarget(s)}
                          className="btn-ghost p-1.5 text-gray-500 hover:text-red-600" title="Wymuś wylogowanie">
                          <Trash2 size={14} />
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        <Pagination page={page} totalPages={data?.totalPages || 0} onPageChange={setPage} />
      </div>

      <ConfirmModal open={!!revokeTarget} onClose={() => setRevokeTarget(null)}
        onConfirm={() => revoke.mutate(revokeTarget.id)}
        title="Wymusić wylogowanie?"
        message={`Sesja użytkownika ${revokeTarget?.username} zostanie natychmiast unieważniona.`}
        danger />
    </div>
  );
}

