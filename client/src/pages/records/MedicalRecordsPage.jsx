import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'react-toastify';
import { Plus, Search, Trash2, FolderOpen, Eye } from 'lucide-react';
import { medicalRecordApi } from '../../api';
import { LoadingOverlay } from '../../components/Spinner';
import { Pagination } from '../../components/Pagination';
import { Modal, ConfirmModal } from '../../components/Modal';
import { format } from 'date-fns';
import { pl } from 'date-fns/locale';

const recordSchema = z.object({
  patientId: z.string().uuid('Nieprawidłowe ID pacjenta'),
  recordType: z.string().min(1, 'Wymagane').max(50),
  contentText: z.string().max(10000).optional().or(z.literal('')),
});

const recordTypes = ['DIAGNOSIS', 'NOTE', 'PROCEDURE', 'REFERRAL', 'SUMMARY', 'OTHER'];

function RecordForm({ defaultValues, onSubmit, submitting }) {
  const { register, handleSubmit, formState: { errors } } = useForm({
    resolver: zodResolver(recordSchema),
    defaultValues: defaultValues || { recordType: 'NOTE' },
  });
  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
      <div>
        <label className="label">ID Pacjenta (UUID) *</label>
        <input {...register('patientId')} className={`input font-mono ${errors.patientId ? 'input-error' : ''}`} placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" />
        {errors.patientId && <p className="error-msg">{errors.patientId.message}</p>}
      </div>
      <div>
        <label className="label">Typ rekordu *</label>
        <select {...register('recordType')} className={`input ${errors.recordType ? 'input-error' : ''}`}>
          {recordTypes.map(t => <option key={t} value={t}>{t}</option>)}
        </select>
        {errors.recordType && <p className="error-msg">{errors.recordType.message}</p>}
      </div>
      <div>
        <label className="label">Treść notatki</label>
        <textarea {...register('contentText')} rows={6} className="input font-mono text-sm" placeholder="Treść rekordu medycznego..." />
      </div>
      <div className="flex justify-end gap-3 pt-2">
        <button type="submit" className="btn-primary" disabled={submitting}>{submitting ? 'Zapisywanie...' : 'Zapisz'}</button>
      </div>
    </form>
  );
}

export default function MedicalRecordsPage() {
  const qc = useQueryClient();
  const [searchParams] = useSearchParams();
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState({ patientId: searchParams.get('patientId') || '', type: '' });
  const [activeFilters, setActiveFilters] = useState({ patientId: searchParams.get('patientId') || '' });
  const [createOpen, setCreateOpen] = useState(false);
  const [viewRecord, setViewRecord] = useState(null);
  const [deleteRecord, setDeleteRecord] = useState(null);

  const { data, isLoading } = useQuery({
    queryKey: ['medicalRecords', 'list', activeFilters, page],
    queryFn: () => medicalRecordApi.list(activeFilters, { page, size: 10, sort: 'createdAt,desc' }).then(r => r.data),
    keepPreviousData: true,
  });

  const createMutation = useMutation({
    mutationFn: (d) => medicalRecordApi.create({
      patientId: d.patientId,
      recordType: d.recordType,
      content: d.contentText ? { text: d.contentText } : {},
    }),
    onSuccess: () => { qc.invalidateQueries(['medicalRecords']); setCreateOpen(false); toast.success('Rekord dodany'); },
    onError: (e) => toast.error(e.response?.data?.message || 'Błąd tworzenia rekordu'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id) => medicalRecordApi.remove(id),
    onSuccess: () => { qc.invalidateQueries(['medicalRecords']); setDeleteRecord(null); toast.success('Rekord usunięty'); },
    onError: (e) => toast.error(e.response?.data?.message || 'Błąd usuwania'),
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="page-title">Rekordy medyczne</h1>
          <p className="text-sm text-gray-500 mt-1">Dokumentacja medyczna pacjentów</p>
        </div>
        <button className="btn-primary" onClick={() => setCreateOpen(true)}>
          <Plus size={16} /> Nowy rekord
        </button>
      </div>

      <div className="card py-4">
        <div className="flex flex-wrap gap-3">
          <input className="input w-72" placeholder="ID pacjenta (UUID)" value={filters.patientId}
            onChange={e => setFilters(f => ({ ...f, patientId: e.target.value }))} />
          <select className="input w-40" value={filters.type} onChange={e => setFilters(f => ({ ...f, type: e.target.value }))}>
            <option value="">Wszystkie typy</option>
            {recordTypes.map(t => <option key={t} value={t}>{t}</option>)}
          </select>
          <button className="btn-primary" onClick={() => { setActiveFilters({ ...filters }); setPage(0); }}>
            <Search size={15} /> Szukaj
          </button>
          <button className="btn-secondary" onClick={() => { setFilters({ patientId: '', type: '' }); setActiveFilters({}); setPage(0); }}>
            Wyczyść
          </button>
        </div>
      </div>

      <div className="card p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr>
                <th className="table-header">Typ</th>
                <th className="table-header">Treść (podgląd)</th>
                <th className="table-header">Data utworzenia</th>
                <th className="table-header">Szyfrowany</th>
                <th className="table-header text-right">Akcje</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {isLoading ? (
                <tr><td colSpan={5}><LoadingOverlay /></td></tr>
              ) : data?.content?.length === 0 ? (
                <tr>
                  <td colSpan={5} className="text-center py-12 text-gray-400">
                    <FolderOpen size={32} className="mx-auto mb-2 opacity-40" />
                    <p className="text-sm">Brak rekordów</p>
                  </td>
                </tr>
              ) : (
                data?.content?.map((r) => (
                  <tr key={r.id} className="hover:bg-gray-50 transition-colors">
                    <td className="table-cell">
                      <span className="badge badge-blue">{r.recordType}</span>
                    </td>
                    <td className="table-cell max-w-sm">
                      <p className="truncate text-xs text-gray-600">
                        {r.content?.text || JSON.stringify(r.content)?.substring(0, 80) || '—'}
                      </p>
                    </td>
                    <td className="table-cell text-xs">
                      {r.createdAt ? format(new Date(r.createdAt), 'dd.MM.yyyy HH:mm', { locale: pl }) : '—'}
                    </td>
                    <td className="table-cell">
                      <span className={`badge ${r.isEncrypted ? 'badge-green' : 'badge-gray'}`}>
                        {r.isEncrypted ? 'Tak' : 'Nie'}
                      </span>
                    </td>
                    <td className="table-cell">
                      <div className="flex items-center justify-end gap-2">
                        <button className="btn-ghost p-1.5 text-gray-500 hover:text-primary-600" onClick={() => setViewRecord(r)} title="Podgląd">
                          <Eye size={15} />
                        </button>
                        <button className="btn-ghost p-1.5 text-gray-500 hover:text-red-600" onClick={() => setDeleteRecord(r)} title="Usuń">
                          <Trash2 size={15} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        <Pagination page={page} totalPages={data?.totalPages || 0} onPageChange={setPage} />
      </div>

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Nowy rekord medyczny" size="lg">
        <RecordForm onSubmit={(d) => createMutation.mutate(d)} submitting={createMutation.isPending} />
      </Modal>

      <Modal open={!!viewRecord} onClose={() => setViewRecord(null)} title={`Rekord: ${viewRecord?.recordType}`} size="lg">
        {viewRecord && (
          <div className="space-y-3">
            <pre className="bg-gray-50 rounded-lg p-4 text-xs font-mono overflow-auto max-h-96 whitespace-pre-wrap">
              {JSON.stringify(viewRecord.content, null, 2)}
            </pre>
            <p className="text-xs text-gray-400">
              Utworzono: {viewRecord.createdAt ? format(new Date(viewRecord.createdAt), 'dd.MM.yyyy HH:mm:ss', { locale: pl }) : '—'}
            </p>
          </div>
        )}
      </Modal>

      <ConfirmModal open={!!deleteRecord} onClose={() => setDeleteRecord(null)}
        onConfirm={() => deleteMutation.mutate(deleteRecord.id)}
        title="Usuń rekord" message="Czy na pewno chcesz usunąć ten rekord medyczny?" danger />
    </div>
  );
}
