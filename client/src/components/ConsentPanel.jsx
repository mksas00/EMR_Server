/**
 * ConsentPanel – panel zgód dostępu do danych pacjenta (RODO).
 * Wbudowany w PatientDetailPage. Pokazuje aktywne zgody i pozwala
 * lekarzowi/adminowi nadawać/cofać dostęp innym użytkownikom.
 */
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'react-toastify';
import { Shield, Plus, Trash2, Clock, CheckCircle, XCircle, ChevronDown, ChevronUp } from 'lucide-react';
import { format, formatDistanceToNow } from 'date-fns';
import { pl } from 'date-fns/locale';
import { consentApi } from '../api';
import { Modal, ConfirmModal } from './Modal';
import { useAuth } from '../contexts/AuthContext';

const SCOPE_LABEL = { read: 'Odczyt', write: 'Odczyt + Zapis' };
const SCOPE_BADGE = { read: 'badge-blue', write: 'badge-yellow' };

const grantSchema = z.object({
  grantedToUserId: z.string().uuid('Nieprawidłowe ID użytkownika'),
  scope: z.enum(['read', 'write']),
  reason: z.string().max(1000).optional(),
  expiresAt: z.string().optional(),
});

function GrantModal({ open, onClose, patientId }) {
  const qc = useQueryClient();
  const { register, handleSubmit, reset, formState: { errors } } = useForm({
    resolver: zodResolver(grantSchema),
    defaultValues: { scope: 'read' },
  });

  const mutation = useMutation({
    mutationFn: (data) => consentApi.grant(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['consents', patientId] });
      toast.success('Dostęp nadany');
      reset();
      onClose();
    },
    onError: (e) => toast.error(e.response?.data?.message || 'Błąd nadawania dostępu'),
  });

  const onSubmit = (form) => {
    mutation.mutate({
      patientId,
      grantedToUserId: form.grantedToUserId,
      scope: form.scope,
      reason: form.reason || null,
      expiresAt: form.expiresAt ? new Date(form.expiresAt).toISOString() : null,
    });
  };

  return (
    <Modal open={open} onClose={onClose} title="Nadaj dostęp do pacjenta" size="md">
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
        <div>
          <label className="label">ID użytkownika (UUID) *</label>
          <input
            {...register('grantedToUserId')}
            className={`input font-mono ${errors.grantedToUserId ? 'input-error' : ''}`}
            placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
          />
          {errors.grantedToUserId && <p className="error-msg">{errors.grantedToUserId.message}</p>}
          <p className="text-xs text-gray-400 mt-1">UUID lekarza, pielęgniarki lub laboranta</p>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="label">Zakres dostępu</label>
            <select {...register('scope')} className="input">
              <option value="read">Odczyt</option>
              <option value="write">Odczyt + Zapis</option>
            </select>
          </div>
          <div>
            <label className="label">Wygasa (opcjonalne)</label>
            <input
              {...register('expiresAt')}
              type="datetime-local"
              className="input"
              min={new Date().toISOString().slice(0, 16)}
            />
          </div>
        </div>

        <div>
          <label className="label">Uzasadnienie (RODO)</label>
          <textarea
            {...register('reason')}
            className="input"
            rows={2}
            maxLength={1000}
            placeholder="np. Konsultacja kardiologiczna, prośba pacjenta..."
          />
        </div>

        <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 text-xs text-amber-800">
          <Shield size={12} className="inline mr-1" />
          Nadanie dostępu jest rejestrowane w logu audytowym zgodnie z wymogami RODO.
        </div>

        <div className="flex justify-end gap-3 pt-1">
          <button type="button" className="btn-secondary" onClick={onClose}>Anuluj</button>
          <button type="submit" className="btn-primary" disabled={mutation.isPending}>
            {mutation.isPending ? 'Nadawanie...' : 'Nadaj dostęp'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

export default function ConsentPanel({ patientId }) {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [grantOpen, setGrantOpen] = useState(false);
  const [revokeTarget, setRevokeTarget] = useState(null);
  const [collapsed, setCollapsed] = useState(false);

  const canManage = ['doctor', 'admin'].includes(user?.role);

  const { data } = useQuery({
    queryKey: ['consents', patientId],
    queryFn: () => consentApi.listForPatient(patientId, { page: 0, size: 20, sort: 'grantedAt,desc' }).then(r => r.data),
    enabled: !!patientId,
  });

  const revokeMutation = useMutation({
    mutationFn: (id) => consentApi.revoke(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['consents', patientId] });
      toast.success('Dostęp cofnięty');
      setRevokeTarget(null);
    },
    onError: (e) => toast.error(e.response?.data?.message || 'Błąd cofania dostępu'),
  });

  const consents = data?.content || [];
  const activeCount = consents.filter(c => c.active).length;

  return (
    <div className="card">
      <div
        className="flex items-center justify-between mb-0 cursor-pointer select-none"
        onClick={() => setCollapsed(v => !v)}
      >
        <h2 className="section-title flex items-center gap-2">
          <Shield size={16} className="text-indigo-600" />
          Zgody dostępu
          {activeCount > 0 && (
            <span className="badge badge-blue text-xs ml-1">{activeCount} aktywnych</span>
          )}
        </h2>
        <div className="flex items-center gap-2" onClick={e => e.stopPropagation()}>
          {canManage && (
            <button
              className="btn-primary text-xs py-1"
              onClick={() => setGrantOpen(true)}
            >
              <Plus size={12} /> Nadaj dostęp
            </button>
          )}
          {collapsed ? <ChevronDown size={16} className="text-gray-400" /> : <ChevronUp size={16} className="text-gray-400" />}
        </div>
      </div>

      {!collapsed && (
        <div className="mt-4 space-y-2">
          {consents.length === 0 ? (
            <p className="text-sm text-gray-400 py-2">Brak nadanych zgód dostępu</p>
          ) : (
            consents.map(c => (
              <div
                key={c.id}
                className={`flex items-start justify-between p-3 rounded-lg border ${
                  c.active ? 'border-indigo-100 bg-indigo-50' : 'border-gray-100 bg-gray-50 opacity-60'
                }`}
              >
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="font-medium text-sm">{c.grantedToUsername}</span>
                    <span className={`badge text-xs ${SCOPE_BADGE[c.scope] || 'badge-gray'}`}>
                      {SCOPE_LABEL[c.scope] || c.scope}
                    </span>
                    {c.grantedToRole && (
                      <span className="badge badge-gray text-xs">{c.grantedToRole}</span>
                    )}
                    {c.active ? (
                      <span className="flex items-center gap-1 text-xs text-green-600">
                        <CheckCircle size={11} /> Aktywna
                      </span>
                    ) : (
                      <span className="flex items-center gap-1 text-xs text-gray-400">
                        <XCircle size={11} /> Nieaktywna
                      </span>
                    )}
                  </div>

                  <div className="text-xs text-gray-500 mt-1 space-y-0.5">
                    <p>
                      Nadano: {c.grantedAt ? format(new Date(c.grantedAt), 'dd.MM.yyyy HH:mm') : '—'}
                    </p>
                    {c.expiresAt && (
                      <p className="flex items-center gap-1">
                        <Clock size={10} />
                        Wygasa: {format(new Date(c.expiresAt), 'dd.MM.yyyy HH:mm')}
                        {c.active && (
                          <span className="text-amber-600">
                            {' '}({formatDistanceToNow(new Date(c.expiresAt), { locale: pl, addSuffix: true })})
                          </span>
                        )}
                      </p>
                    )}
                    {c.revokedAt && (
                      <p className="text-red-500">
                        Cofnięto: {format(new Date(c.revokedAt), 'dd.MM.yyyy HH:mm')}
                      </p>
                    )}
                    {c.reason && (
                      <p className="text-gray-400 italic truncate max-w-xs" title={c.reason}>
                        {c.reason}
                      </p>
                    )}
                  </div>
                </div>

                {canManage && c.active && (
                  <button
                    className="btn-ghost p-1.5 text-red-400 hover:text-red-600 flex-shrink-0 ml-2"
                    onClick={() => setRevokeTarget(c)}
                    title="Cofnij dostęp"
                  >
                    <Trash2 size={14} />
                  </button>
                )}
              </div>
            ))
          )}
        </div>
      )}

      <GrantModal open={grantOpen} onClose={() => setGrantOpen(false)} patientId={patientId} />

      <ConfirmModal
        open={!!revokeTarget}
        onClose={() => setRevokeTarget(null)}
        onConfirm={() => revokeMutation.mutate(revokeTarget?.id)}
        title="Cofnij dostęp"
        message={
          <span>
            Czy na pewno cofnąć dostęp użytkownika <strong>{revokeTarget?.grantedToUsername}</strong> do tego pacjenta?
            Operacja zostanie zapisana w logu audytowym.
          </span>
        }
        danger
      />
    </div>
  );
}

