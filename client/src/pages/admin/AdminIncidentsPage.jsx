import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { AlertTriangle, Search, X, Check } from 'lucide-react';
import { toast } from 'react-toastify';
import { adminApi } from '../../api';
import { LoadingOverlay } from '../../components/Spinner';
import { Pagination } from '../../components/Pagination';
import { format } from 'date-fns';

const SEVERITY_BADGE = {
  low: 'badge-blue',
  medium: 'badge-yellow',
  high: 'badge-red',
  critical: 'badge-red',
};

const STATUS_BADGE = {
  open: 'badge-red',
  investigating: 'badge-yellow',
  resolved: 'badge-green',
  false_positive: 'badge-gray',
};

const STATUS_OPTIONS = [
  { value: 'open', label: 'Otwarty' },
  { value: 'investigating', label: 'W trakcie analizy' },
  { value: 'resolved', label: 'Rozwiązany' },
  { value: 'false_positive', label: 'False positive' },
];

export default function AdminIncidentsPage() {
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState({ status: '', severity: '', category: '' });
  // Track optimistic status per incident id to avoid visual jumps during refetch
  const [optimisticStatus, setOptimisticStatus] = useState({});
  // Track which incident id is currently being saved
  const [savingId, setSavingId] = useState(null);

  const { data, isLoading } = useQuery({
    queryKey: ['admin', 'incidents', page, filters],
    queryFn: () => adminApi.incidents({
      page, size: 50,
      ...(filters.status && { status: filters.status }),
      ...(filters.severity && { severity: filters.severity }),
      ...(filters.category && { category: filters.category }),
    }).then(r => r.data),
    keepPreviousData: true,
    onSuccess: () => {
      // Clear optimistic overrides once fresh data arrives
      setOptimisticStatus({});
    },
  });

  const updateStatus = useMutation({
    mutationFn: ({ id, status }) => adminApi.updateIncidentStatus(id, status),
    onMutate: ({ id, status }) => {
      setSavingId(id);
      setOptimisticStatus(prev => ({ ...prev, [id]: status }));
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin', 'incidents'] });
      qc.invalidateQueries({ queryKey: ['admin', 'dashboard'] });
      toast.success('Status zmieniony');
    },
    onError: (e, { id }) => {
      // Rollback optimistic update on error
      setOptimisticStatus(prev => {
        const next = { ...prev };
        delete next[id];
        return next;
      });
      toast.error(e.response?.data?.message || 'Błąd zmiany statusu');
    },
    onSettled: () => setSavingId(null),
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <AlertTriangle size={22} className="text-orange-500" />
        <h1 className="page-title">Incydenty bezpieczeństwa</h1>
      </div>

      {/* Filters */}
      <div className="card p-4 grid grid-cols-1 md:grid-cols-4 gap-3">
        <div>
          <label className="label text-xs">Status</label>
          <select value={filters.status} onChange={e => { setFilters({ ...filters, status: e.target.value }); setPage(0); }}
            className="input">
            <option value="">Wszystkie</option>
            {STATUS_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
          </select>
        </div>
        <div>
          <label className="label text-xs">Ważność</label>
          <select value={filters.severity} onChange={e => { setFilters({ ...filters, severity: e.target.value }); setPage(0); }}
            className="input">
            <option value="">Wszystkie</option>
            <option value="low">Niska</option>
            <option value="medium">Średnia</option>
            <option value="high">Wysoka</option>
            <option value="critical">Krytyczna</option>
          </select>
        </div>
        <div>
          <label className="label text-xs">Kategoria</label>
          <input value={filters.category} onChange={e => { setFilters({ ...filters, category: e.target.value }); setPage(0); }}
            className="input" placeholder="np. ACCOUNT_LOCK" />
        </div>
        <div className="flex items-end">
          {(filters.status || filters.severity || filters.category) && (
            <button onClick={() => { setFilters({ status: '', severity: '', category: '' }); setPage(0); }}
              className="btn-ghost text-gray-500"><X size={14} /> Wyczyść</button>
          )}
        </div>
      </div>

      {/* Table */}
      <div className="card p-0 overflow-hidden">
        {isLoading && <LoadingOverlay />}
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr>
                <th className="table-header">Wykryty</th>
                <th className="table-header">Ważność</th>
                <th className="table-header">Kategoria</th>
                <th className="table-header">Opis</th>
                <th className="table-header">Użytkownik</th>
                <th className="table-header">Status</th>
                <th className="table-header text-right">Akcje</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {!isLoading && data?.content?.length === 0 ? (
                <tr><td colSpan={7} className="text-center py-12 text-gray-400">
                  <AlertTriangle size={36} className="mx-auto mb-2 opacity-30" />
                  <p className="text-sm">Brak incydentów</p>
                </td></tr>
              ) : (
                data?.content?.map(inc => {
                  const currentStatus = optimisticStatus[inc.id] ?? inc.status ?? 'open';
                  const isSaving = savingId === inc.id;
                  return (
                    <tr key={inc.id} className="hover:bg-gray-50">
                      <td className="table-cell font-mono text-xs whitespace-nowrap">
                        {inc.detectedAt ? format(new Date(inc.detectedAt), 'yyyy-MM-dd HH:mm') : '—'}
                      </td>
                      <td className="table-cell">
                        <span className={`badge ${SEVERITY_BADGE[inc.severity] || 'badge-gray'} capitalize`}>
                          {inc.severity}
                        </span>
                      </td>
                      <td className="table-cell text-xs font-mono text-gray-600">{inc.category}</td>
                      <td className="table-cell text-sm text-gray-700 max-w-md">
                        <p className="line-clamp-2">{inc.description || '—'}</p>
                      </td>
                      <td className="table-cell text-sm">{inc.username || <span className="text-gray-400">—</span>}</td>
                      <td className="table-cell">
                        <span className={`badge ${STATUS_BADGE[currentStatus] || 'badge-gray'}`}>
                          {STATUS_OPTIONS.find(o => o.value === currentStatus)?.label || currentStatus}
                        </span>
                      </td>
                      <td className="table-cell">
                        <select
                          value={currentStatus}
                          disabled={isSaving}
                          onChange={e => updateStatus.mutate({ id: inc.id, status: e.target.value })}
                          className={`text-xs border rounded px-2 py-1 bg-white ${isSaving ? 'opacity-50 cursor-not-allowed' : ''}`}
                        >
                          {STATUS_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
                        </select>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
        <Pagination page={page} totalPages={data?.totalPages || 0} onPageChange={setPage} />
      </div>
    </div>
  );
}

