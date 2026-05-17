import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { Users, Search, X, Lock, Unlock, ShieldCheck, ShieldOff } from 'lucide-react';
import { toast } from 'react-toastify';
import { adminApi } from '../../api';
import { LoadingOverlay } from '../../components/Spinner';
import { Pagination } from '../../components/Pagination';
import { ConfirmModal } from '../../components/Modal';
import { format } from 'date-fns';

const ROLE_BADGE = {
  admin: 'badge-red',
  doctor: 'badge-blue',
  nurse: 'badge-green',
  lab_tech: 'badge-yellow',
  patient: 'badge-gray',
};

export default function AdminUsersPage() {
  const qc = useQueryClient();
  const [searchParams] = useSearchParams();
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState({
    q: '',
    role: '',
    locked: searchParams.get('locked') === 'true' ? 'true' : '',
  });
  const [draft, setDraft] = useState(filters);
  const [actionTarget, setActionTarget] = useState(null); // { user, action: 'lock'|'unlock' }

  const { data, isLoading } = useQuery({
    queryKey: ['admin', 'users', page, filters],
    queryFn: () => adminApi.users({
      page, size: 50,
      ...(filters.q && { q: filters.q }),
      ...(filters.role && { role: filters.role }),
      ...(filters.locked !== '' && { locked: filters.locked === 'true' }),
    }).then(r => r.data),
    keepPreviousData: true,
  });

  const lockMutation = useMutation({
    mutationFn: id => adminApi.lockUser(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin', 'users'] });
      qc.invalidateQueries({ queryKey: ['admin', 'dashboard'] });
      setActionTarget(null);
      toast.success('Konto zablokowane');
    },
    onError: e => toast.error(e.response?.data?.message || 'Błąd'),
  });

  const unlockMutation = useMutation({
    mutationFn: id => adminApi.unlockUser(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin', 'users'] });
      qc.invalidateQueries({ queryKey: ['admin', 'dashboard'] });
      setActionTarget(null);
      toast.success('Konto odblokowane');
    },
    onError: e => toast.error(e.response?.data?.message || 'Błąd'),
  });

  const apply = e => {
    e.preventDefault();
    setFilters(draft);
    setPage(0);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Users size={22} className="text-primary-600" />
        <h1 className="page-title">Zarządzanie kontami</h1>
      </div>

      <form onSubmit={apply} className="card p-4 grid grid-cols-1 md:grid-cols-4 gap-3">
        <div className="md:col-span-2">
          <label className="label text-xs">Szukaj (login/email)</label>
          <div className="relative">
            <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
            <input value={draft.q} onChange={e => setDraft({ ...draft, q: e.target.value })}
              className="input pl-9" placeholder="np. admin..." />
          </div>
        </div>
        <div>
          <label className="label text-xs">Rola</label>
          <select value={draft.role} onChange={e => setDraft({ ...draft, role: e.target.value })} className="input">
            <option value="">Wszystkie</option>
            <option value="admin">Administrator</option>
            <option value="doctor">Lekarz</option>
            <option value="nurse">Pielęgniarka</option>
            <option value="lab_tech">Laborant</option>
            <option value="patient">Pacjent</option>
          </select>
        </div>
        <div>
          <label className="label text-xs">Status</label>
          <select value={draft.locked} onChange={e => setDraft({ ...draft, locked: e.target.value })} className="input">
            <option value="">Wszystkie</option>
            <option value="false">Aktywne</option>
            <option value="true">Zablokowane</option>
          </select>
        </div>
        <div className="md:col-span-4 flex gap-2">
          <button type="submit" className="btn-primary"><Search size={14} /> Filtruj</button>
          {(filters.q || filters.role || filters.locked !== '') && (
            <button type="button" onClick={() => { setDraft({ q: '', role: '', locked: '' }); setFilters({ q: '', role: '', locked: '' }); setPage(0); }}
              className="btn-ghost text-gray-500"><X size={14} /> Wyczyść</button>
          )}
        </div>
      </form>

      <div className="card p-0 overflow-hidden">
        {isLoading && <LoadingOverlay />}
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr>
                <th className="table-header">Login</th>
                <th className="table-header">Email</th>
                <th className="table-header">Rola</th>
                <th className="table-header">MFA</th>
                <th className="table-header">Status</th>
                <th className="table-header">Ostatnie logowanie</th>
                <th className="table-header text-right">Akcje</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {!isLoading && data?.content?.length === 0 ? (
                <tr><td colSpan={7} className="text-center py-12 text-gray-400">
                  <Users size={36} className="mx-auto mb-2 opacity-30" />
                  <p className="text-sm">Brak użytkowników</p>
                </td></tr>
              ) : (
                data?.content?.map(u => (
                  <tr key={u.id} className="hover:bg-gray-50">
                    <td className="table-cell text-sm font-medium">{u.username}</td>
                    <td className="table-cell text-sm text-gray-600">{u.email}</td>
                    <td className="table-cell">
                      <span className={`badge ${ROLE_BADGE[u.role] || 'badge-gray'} capitalize text-xs`}>
                        {u.role}
                      </span>
                    </td>
                    <td className="table-cell">
                      {u.mfaEnabled ? (
                        <ShieldCheck size={16} className="text-green-600" />
                      ) : (
                        <ShieldOff size={16} className="text-gray-300" />
                      )}
                    </td>
                    <td className="table-cell">
                      {u.accountLocked ? (
                        <span className="badge badge-red flex items-center gap-1 w-fit">
                          <Lock size={11} /> Zablokowane
                        </span>
                      ) : (
                        <span className="badge badge-green">Aktywne</span>
                      )}
                      {u.failedLoginAttempts > 0 && !u.accountLocked && (
                        <span className="ml-1 text-xs text-orange-600">({u.failedLoginAttempts} prób)</span>
                      )}
                    </td>
                    <td className="table-cell text-xs text-gray-500">
                      {u.lastLoginAt ? (
                        <>
                          <p className="font-mono">{format(new Date(u.lastLoginAt), 'yyyy-MM-dd HH:mm')}</p>
                          {u.lastLoginIp && <p className="text-gray-400 font-mono">{u.lastLoginIp}</p>}
                        </>
                      ) : <span className="text-gray-400">—</span>}
                    </td>
                    <td className="table-cell text-right">
                      {u.accountLocked ? (
                        <button onClick={() => setActionTarget({ user: u, action: 'unlock' })}
                          className="btn-ghost p-1.5 text-gray-500 hover:text-green-600" title="Odblokuj">
                          <Unlock size={14} />
                        </button>
                      ) : (
                        <button onClick={() => setActionTarget({ user: u, action: 'lock' })}
                          className="btn-ghost p-1.5 text-gray-500 hover:text-red-600" title="Zablokuj">
                          <Lock size={14} />
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

      <ConfirmModal open={!!actionTarget} onClose={() => setActionTarget(null)}
        onConfirm={() => {
          if (actionTarget.action === 'lock') lockMutation.mutate(actionTarget.user.id);
          else unlockMutation.mutate(actionTarget.user.id);
        }}
        title={actionTarget?.action === 'lock' ? 'Zablokować konto?' : 'Odblokować konto?'}
        message={`Konto użytkownika ${actionTarget?.user?.username} zostanie ${actionTarget?.action === 'lock' ? 'zablokowane' : 'odblokowane'}.`}
        danger={actionTarget?.action === 'lock'} />
    </div>
  );
}

