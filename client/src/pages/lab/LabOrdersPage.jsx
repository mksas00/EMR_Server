import { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Plus, Search, FlaskConical, AlertTriangle, X, ArrowLeft } from 'lucide-react';
import { format } from 'date-fns';
import { labOrderApi } from '../../api';
import { LoadingOverlay } from '../../components/Spinner';
import { Pagination } from '../../components/Pagination';
import LabOrderCreateModal from '../../components/LabOrderCreateModal';

const STATUS_BADGE = {
  pending: 'badge-yellow',
  in_progress: 'badge-blue',
  completed: 'badge-green',
  cancelled: 'badge-gray',
};
const STATUS_LABEL = {
  pending: 'Oczekujące',
  in_progress: 'W trakcie',
  completed: 'Zakończone',
  cancelled: 'Anulowane',
};
const PRIORITY_BADGE = { routine: 'badge-gray', urgent: 'badge-yellow', stat: 'badge-red' };
const PRIORITY_LABEL = { routine: 'Rutynowe', urgent: 'Pilne', stat: 'CITO' };


// ─── Strona główna: lista zleceń ──────────────────────────────────────
export default function LabOrdersPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const urlPatientId = searchParams.get('patientId');

  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState({ pesel: '', status: '', priority: '' });
  const [activeFilters, setActiveFilters] = useState(urlPatientId ? { patientId: urlPatientId } : {});
  const [createOpen, setCreateOpen] = useState(false);

  // When patientId appears in URL, keep it active
  useEffect(() => {
    if (urlPatientId) {
      setActiveFilters(f => ({ ...f, patientId: urlPatientId }));
      setPage(0);
    }
  }, [urlPatientId]);

  const { data, isLoading } = useQuery({
    queryKey: ['labOrders', 'list', activeFilters, page],
    queryFn: () => labOrderApi.list(activeFilters, { page, size: 15, sort: 'orderedAt,desc' }).then(r => r.data),
    keepPreviousData: true,
  });

  const applyFilters = () => {
    const a = {};
    if (urlPatientId) a.patientId = urlPatientId;
    if (filters.pesel) a.pesel = filters.pesel;
    if (filters.status) a.status = filters.status;
    if (filters.priority) a.priority = filters.priority;
    setActiveFilters(a);
    setPage(0);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          {urlPatientId && (
            <Link to={`/patients/${urlPatientId}`} className="btn-ghost p-2 text-gray-500">
              <ArrowLeft size={18} />
            </Link>
          )}
          <div>
            <h1 className="page-title flex items-center gap-2">
              <FlaskConical size={22} /> Zlecenia laboratoryjne
            </h1>
            {urlPatientId ? (
              <p className="text-sm text-primary-600 mt-0.5">
                Filtr: pacjent {urlPatientId.slice(0, 8)}…
                <Link to="/lab-orders" className="ml-2 text-gray-400 hover:text-gray-600 text-xs">(pokaż wszystkie)</Link>
              </p>
            ) : (
              <p className="text-sm text-gray-500 mt-1">Zlecenia badań i wprowadzanie wyników</p>
            )}
          </div>
        </div>
        <button className="btn-primary" onClick={() => setCreateOpen(true)}>
          <Plus size={16} /> Nowe zlecenie
        </button>
      </div>

      <div className="card py-4">
        <div className="flex flex-wrap gap-3">
          <input className="input w-36" placeholder="PESEL" maxLength={11} inputMode="numeric"
            value={filters.pesel}
            onChange={e => setFilters(f => ({ ...f, pesel: e.target.value.replace(/\D/g, '') }))} />
          <select className="input w-40" value={filters.status}
            onChange={e => setFilters(f => ({ ...f, status: e.target.value }))}>
            <option value="">Wszystkie statusy</option>
            <option value="pending">Oczekujące</option>
            <option value="in_progress">W trakcie</option>
            <option value="completed">Zakończone</option>
            <option value="cancelled">Anulowane</option>
          </select>
          <select className="input w-36" value={filters.priority}
            onChange={e => setFilters(f => ({ ...f, priority: e.target.value }))}>
            <option value="">Każdy priorytet</option>
            <option value="routine">Rutynowy</option>
            <option value="urgent">Pilny</option>
            <option value="stat">CITO</option>
          </select>
          <button className="btn-primary" onClick={applyFilters}>
            <Search size={15} /> Szukaj
          </button>
          <button className="btn-secondary" onClick={() => {
            setFilters({ pesel: '', status: '', priority: '' });
            setActiveFilters({});
            setPage(0);
          }}><X size={14} /> Wyczyść</button>
        </div>
      </div>

      <div className="card p-0 overflow-hidden">
        {isLoading && <LoadingOverlay />}
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr>
                <th className="table-header">Data</th>
                <th className="table-header">Pacjent</th>
                <th className="table-header">Zlecił</th>
                <th className="table-header">Priorytet</th>
                <th className="table-header">Status</th>
                <th className="table-header">Postęp</th>
                <th className="table-header">Nieprawidłowe</th>
                <th className="table-header">Notatka</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {!isLoading && (data?.content?.length === 0 || !data?.content) ? (
                <tr><td colSpan={8} className="text-center py-12 text-gray-400">
                  <FlaskConical size={36} className="mx-auto mb-2 opacity-30" />
                  <p className="text-sm">Brak zleceń</p>
                </td></tr>
              ) : (
                data?.content?.filter(Boolean).map(o => (
                  <tr key={o.id} className="hover:bg-gray-50 cursor-pointer transition-colors"
                      onClick={() => navigate(`/lab-orders/${o.id}`)}>
                    <td className="table-cell font-mono text-xs whitespace-nowrap">
                      {o.orderedAt ? format(new Date(o.orderedAt), 'yyyy-MM-dd HH:mm') : '—'}
                    </td>
                    <td className="table-cell text-sm">
                      <Link to={`/patients/${o.patientId}`} className="text-primary-600 hover:underline"
                            onClick={e => e.stopPropagation()}>
                        {o.patientFullName || o.patientId?.slice(0, 8)}
                      </Link>
                    </td>
                    <td className="table-cell text-xs text-gray-600">{o.orderedByUsername || '—'}</td>
                    <td className="table-cell">
                      <span className={`badge ${PRIORITY_BADGE[o.priority] || 'badge-gray'}`}>
                        {PRIORITY_LABEL[o.priority] || o.priority}
                      </span>
                    </td>
                    <td className="table-cell">
                      <span className={`badge ${STATUS_BADGE[o.status] || 'badge-gray'}`}>
                        {STATUS_LABEL[o.status] || o.status}
                      </span>
                    </td>
                    <td className="table-cell text-xs text-gray-600">
                      {o.completedItemCount}/{o.itemCount}
                    </td>
                    <td className="table-cell">
                      {o.abnormalItemCount > 0 ? (
                        <span className="badge badge-red flex items-center gap-1 w-fit">
                          <AlertTriangle size={12} /> {o.abnormalItemCount}
                        </span>
                      ) : <span className="text-xs text-gray-400">—</span>}
                    </td>
                    <td className="table-cell text-xs text-gray-500 max-w-xs truncate">{o.notes || '—'}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        <Pagination page={page} totalPages={data?.totalPages || 0} onPageChange={setPage} />
      </div>

      <LabOrderCreateModal open={createOpen} onClose={() => setCreateOpen(false)} />
    </div>
  );
}

