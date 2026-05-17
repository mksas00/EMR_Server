import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'react-toastify';
import { Plus, Search, Edit2, Trash2, Download, FileText } from 'lucide-react';
import { prescriptionApi } from '../../api';
import { LoadingOverlay } from '../../components/Spinner';
import { Pagination } from '../../components/Pagination';
import { Modal, ConfirmModal } from '../../components/Modal';

const prescriptionSchema = z.object({
  patientId: z.string().uuid('Nieprawidłowe ID pacjenta'),
  issuedDate: z.string().min(1, 'Wymagane'),
  expirationDate: z.string().optional().or(z.literal('')),
  dosageInfo: z.string().max(1000).optional().or(z.literal('')),
  isRepeatable: z.boolean().optional(),
});

function PrescriptionForm({ defaultValues, onSubmit, submitting }) {
  const { register, handleSubmit, formState: { errors } } = useForm({
    resolver: zodResolver(prescriptionSchema),
    defaultValues: defaultValues || { isRepeatable: false },
  });
  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
      <div>
        <label className="label">ID Pacjenta (UUID) *</label>
        <input {...register('patientId')} className={`input font-mono ${errors.patientId ? 'input-error' : ''}`} placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" />
        {errors.patientId && <p className="error-msg">{errors.patientId.message}</p>}
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="label">Data wystawienia *</label>
          <input {...register('issuedDate')} type="date" className={`input ${errors.issuedDate ? 'input-error' : ''}`} />
          {errors.issuedDate && <p className="error-msg">{errors.issuedDate.message}</p>}
        </div>
        <div>
          <label className="label">Ważna do</label>
          <input {...register('expirationDate')} type="date" className="input" />
        </div>
      </div>
      <div>
        <label className="label">Zalecenia dawkowania</label>
        <textarea {...register('dosageInfo')} rows={3} className="input" placeholder="Ogólne instrukcje dotyczące dawkowania..." />
      </div>
      <div className="flex items-center gap-2">
        <input {...register('isRepeatable')} type="checkbox" id="repeatable" className="rounded border-gray-300 text-primary-600" />
        <label htmlFor="repeatable" className="text-sm text-gray-700">Recepta powtarzalna</label>
      </div>
      <div className="flex justify-end gap-3 pt-2">
        <button type="submit" className="btn-primary" disabled={submitting}>{submitting ? 'Zapisywanie...' : 'Zapisz'}</button>
      </div>
    </form>
  );
}

export default function PrescriptionsPage() {
  const qc = useQueryClient();
  const [searchParams] = useSearchParams();
  const [page, setPage] = useState(0);
  const initialPatientId = searchParams.get('patientId') || '';
  const [filters, setFilters] = useState({ pesel: '', patientId: initialPatientId });
  const [activeFilters, setActiveFilters] = useState({ patientId: initialPatientId });
  const [createOpen, setCreateOpen] = useState(false);
  const [editRx, setEditRx] = useState(null);
  const [deleteRx, setDeleteRx] = useState(null);

  const { data, isLoading } = useQuery({
    queryKey: ['prescriptions', 'list', activeFilters, page],
    queryFn: () => prescriptionApi.list(activeFilters, { page, size: 10, sort: 'issuedDate,desc' }).then(r => r.data),
    keepPreviousData: true,
  });

  const createMutation = useMutation({
    mutationFn: (d) => prescriptionApi.create(d),
    onSuccess: () => { qc.invalidateQueries(['prescriptions']); setCreateOpen(false); toast.success('Recepta wystawiona'); },
    onError: (e) => toast.error(e.response?.data?.message || 'Błąd tworzenia recepty'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id) => prescriptionApi.remove(id),
    onSuccess: () => { qc.invalidateQueries(['prescriptions']); setDeleteRx(null); toast.success('Recepta usunięta'); },
    onError: (e) => toast.error(e.response?.data?.message || 'Błąd usuwania'),
  });

  const downloadPdf = async (id) => {
    try {
      const resp = await prescriptionApi.downloadPdf(id);
      const url = URL.createObjectURL(new Blob([resp.data], { type: 'application/pdf' }));
      const a = document.createElement('a'); a.href = url; a.download = `recepta-${id}.pdf`; a.click();
      URL.revokeObjectURL(url);
    } catch { toast.error('Nie można pobrać PDF'); }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="page-title">Recepty</h1>
          <p className="text-sm text-gray-500 mt-1">Zarządzanie receptami pacjentów</p>
        </div>
        <button className="btn-primary" onClick={() => setCreateOpen(true)}>
          <Plus size={16} /> Nowa recepta
        </button>
      </div>

      <div className="card py-4">
        <div className="flex flex-wrap gap-3">
          <input className="input w-36" placeholder="PESEL" maxLength={11} inputMode="numeric"
            value={filters.pesel}
            onChange={e => setFilters(f => ({ ...f, pesel: e.target.value.replace(/\D/g, ''), patientId: '' }))} />
          <button className="btn-primary" onClick={() => {
            const active = {};
            if (filters.pesel) active.pesel = filters.pesel;
            else if (filters.patientId) active.patientId = filters.patientId;
            setActiveFilters(active);
            setPage(0);
          }}>
            <Search size={15} /> Szukaj
          </button>
          <button className="btn-secondary" onClick={() => { setFilters({ pesel: '', patientId: '' }); setActiveFilters({}); setPage(0); }}>
            Wyczyść
          </button>
        </div>
      </div>

      <div className="card p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr>
                <th className="table-header">ID</th>
                <th className="table-header">Data wystawienia</th>
                <th className="table-header">Ważna do</th>
                <th className="table-header">Zalecenia</th>
                <th className="table-header">Powtarzalna</th>
                <th className="table-header text-right">Akcje</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {isLoading ? (
                <tr><td colSpan={6}><LoadingOverlay /></td></tr>
              ) : data?.content?.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center py-12 text-gray-400">
                    <FileText size={32} className="mx-auto mb-2 opacity-40" />
                    <p className="text-sm">Brak recept</p>
                  </td>
                </tr>
              ) : (
                data?.content?.map((rx) => (
                  <tr key={rx.id} className="hover:bg-gray-50 transition-colors">
                    <td className="table-cell font-mono text-xs text-gray-400">{rx.id?.substring(0, 8)}...</td>
                    <td className="table-cell font-medium">{rx.issuedDate || '—'}</td>
                    <td className="table-cell">{rx.expirationDate || '—'}</td>
                    <td className="table-cell max-w-xs">
                      <p className="truncate text-xs">{rx.dosageInfo || '—'}</p>
                    </td>
                    <td className="table-cell">
                      <span className={`badge ${rx.isRepeatable ? 'badge-green' : 'badge-gray'}`}>
                        {rx.isRepeatable ? 'Tak' : 'Nie'}
                      </span>
                    </td>
                    <td className="table-cell">
                      <div className="flex items-center justify-end gap-2">
                        <button className="btn-ghost p-1.5 text-gray-500 hover:text-primary-600" title="Pobierz PDF" onClick={() => downloadPdf(rx.id)}>
                          <Download size={15} />
                        </button>
                        <button className="btn-ghost p-1.5 text-gray-500 hover:text-red-600" title="Usuń" onClick={() => setDeleteRx(rx)}>
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

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Nowa recepta" size="lg">
        <PrescriptionForm onSubmit={(d) => createMutation.mutate(d)} submitting={createMutation.isPending} />
      </Modal>

      <ConfirmModal
        open={!!deleteRx}
        onClose={() => setDeleteRx(null)}
        onConfirm={() => deleteMutation.mutate(deleteRx.id)}
        title="Usuń receptę"
        message="Czy na pewno chcesz usunąć tę receptę?"
        danger
      />
    </div>
  );
}
