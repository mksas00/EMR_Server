import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'react-toastify';
import { Plus, Edit2, Trash2, ArrowLeft, FlaskConical } from 'lucide-react';
import { labResultApi, patientApi } from '../../api';
import { LoadingOverlay } from '../../components/Spinner';
import { Pagination } from '../../components/Pagination';
import { Modal, ConfirmModal } from '../../components/Modal';
const labSchema = z.object({
  testName: z.string().min(1, 'Wymagane').max(200),
  result: z.string().max(500).optional().or(z.literal('')),
  resultDate: z.string().optional().or(z.literal('')),
  unit: z.string().max(20).optional().or(z.literal('')),
  referenceRange: z.string().max(100).optional().or(z.literal('')),
  status: z.enum(['pending', 'completed', 'cancelled', '']).optional(),
});
const statusColor = { completed: 'badge-green', pending: 'badge-yellow', cancelled: 'badge-red' };
const statusLabel = { completed: 'Zakończony', pending: 'Oczekujący', cancelled: 'Anulowany' };
function LabForm({ defaultValues, onSubmit, submitting }) {
  const { register, handleSubmit, formState: { errors } } = useForm({
    resolver: zodResolver(labSchema),
    defaultValues: defaultValues || { status: 'pending' },
  });
  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="label">Nazwa badania *</label>
          <input {...register('testName')} className={`input ${errors.testName ? 'input-error' : ''}`} />
          {errors.testName && <p className="error-msg">{errors.testName.message}</p>}
        </div>
        <div>
          <label className="label">Data wyniku</label>
          <input {...register('resultDate')} type="date" className="input" />
        </div>
      </div>
      <div className="grid grid-cols-3 gap-4">
        <div><label className="label">Wynik</label><input {...register('result')} className="input" /></div>
        <div><label className="label">Jednostka</label><input {...register('unit')} className="input" maxLength={20} /></div>
        <div><label className="label">Norma</label><input {...register('referenceRange')} className="input" /></div>
      </div>
      <div>
        <label className="label">Status</label>
        <select {...register('status')} className="input">
          <option value="pending">Oczekujący</option>
          <option value="completed">Zakończony</option>
          <option value="cancelled">Anulowany</option>
        </select>
      </div>
      <div className="flex justify-end pt-2 border-t border-gray-100">
        <button type="submit" className="btn-primary" disabled={submitting}>{submitting ? 'Zapisywanie...' : 'Zapisz'}</button>
      </div>
    </form>
  );
}
export default function PatientLabResultsPage() {
  const { patientId } = useParams();
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const [filterStatus, setFilterStatus] = useState('');
  const [createOpen, setCreateOpen] = useState(false);
  const [editLab, setEditLab] = useState(null);
  const [deleteLab, setDeleteLab] = useState(null);
  const { data: patient } = useQuery({
    queryKey: ['patient', patientId],
    queryFn: () => patientApi.get(patientId).then(r => r.data),
    enabled: !!patientId,
  });
  const { data, isLoading } = useQuery({
    queryKey: ['labResults', 'patient', patientId, page, filterStatus],
    queryFn: () => labResultApi.list(
      { patientId, ...(filterStatus ? { status: filterStatus } : {}) },
      { page, size: 10, sort: 'resultDate,desc' }
    ).then(r => r.data),
    keepPreviousData: true,
    enabled: !!patientId,
  });
  const createMutation = useMutation({
    mutationFn: d => labResultApi.create({ ...d, patientId }),
    onSuccess: () => { qc.invalidateQueries(['labResults']); setCreateOpen(false); toast.success('Wynik dodany'); },
    onError: e => toast.error(e.response?.data?.message || 'Blad tworzenia wyniku'),
  });
  const updateMutation = useMutation({
    mutationFn: ({ id, data: body }) => labResultApi.update(id, body),
    onSuccess: () => { qc.invalidateQueries(['labResults']); setEditLab(null); toast.success('Wynik zaktualizowany'); },
    onError: e => toast.error(e.response?.data?.message || 'Blad aktualizacji'),
  });
  const deleteMutation = useMutation({
    mutationFn: id => labResultApi.remove(id),
    onSuccess: () => { qc.invalidateQueries(['labResults']); setDeleteLab(null); toast.success('Wynik usuniety'); },
    onError: e => toast.error(e.response?.data?.message || 'Blad usuwania'),
  });
  const patientName = patient ? `${patient.firstName||''} ${patient.lastName||''}`.trim() : '';
  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Link to={`/patients/${patientId}`} className="btn-ghost p-2 text-gray-500"><ArrowLeft size={18} /></Link>
        <div className="flex-1">
          <h1 className="page-title">Wyniki laboratoryjne</h1>
          {patientName && <p className="text-sm text-gray-500">Pacjent: {patientName}</p>}
        </div>
        <button className="btn-primary" onClick={() => setCreateOpen(true)}><Plus size={16} /> Nowy wynik</button>
      </div>
      <div className="card py-3">
        <div className="flex flex-wrap gap-3 items-center">
          <label className="text-sm text-gray-600">Status:</label>
          <select className="input w-40" value={filterStatus} onChange={e => { setFilterStatus(e.target.value); setPage(0); }}>
            <option value="">Wszystkie</option>
            <option value="pending">Oczekujące</option>
            <option value="completed">Zakończone</option>
            <option value="cancelled">Anulowane</option>
          </select>
          {filterStatus && <button className="btn-secondary text-xs py-1" onClick={() => { setFilterStatus(''); setPage(0); }}>Wyczysc</button>}
        </div>
      </div>
      <div className="card p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr>
                <th className="table-header">Badanie</th>
                <th className="table-header">Wynik</th>
                <th className="table-header">Jednostka</th>
                <th className="table-header">Norma</th>
                <th className="table-header">Data</th>
                <th className="table-header">Status</th>
                <th className="table-header text-right">Akcje</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {isLoading ? (<tr><td colSpan={7}><LoadingOverlay /></td></tr>)
              : data?.content?.length === 0 ? (
                <tr><td colSpan={7} className="text-center py-12 text-gray-400">
                  <FlaskConical size={32} className="mx-auto mb-2 opacity-40" />
                  <p className="text-sm">Brak wynikow dla tego pacjenta</p>
                </td></tr>
              ) : (data?.content?.map(l => (
                <tr key={l.id} className="hover:bg-gray-50 transition-colors">
                  <td className="table-cell font-medium">{l.testName}</td>
                  <td className="table-cell">{l.result || '—'}</td>
                  <td className="table-cell text-xs text-gray-500">{l.unit || '—'}</td>
                  <td className="table-cell text-xs text-gray-500">{l.referenceRange || '—'}</td>
                  <td className="table-cell">{l.resultDate || '—'}</td>
                  <td className="table-cell">
                    <span className={`badge ${statusColor[l.status] || 'badge-gray'}`}>{statusLabel[l.status] || l.status || '—'}</span>
                  </td>
                  <td className="table-cell">
                    <div className="flex items-center justify-end gap-2">
                      <button className="btn-ghost p-1.5 text-gray-500 hover:text-primary-600" onClick={() => setEditLab(l)}><Edit2 size={15} /></button>
                      <button className="btn-ghost p-1.5 text-gray-500 hover:text-red-600" onClick={() => setDeleteLab(l)}><Trash2 size={15} /></button>
                    </div>
                  </td>
                </tr>
              )))}
            </tbody>
          </table>
        </div>
        <Pagination page={page} totalPages={data?.totalPages || 0} onPageChange={setPage} />
      </div>
      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Nowy wynik badania" size="lg">
        <LabForm onSubmit={d => createMutation.mutate(d)} submitting={createMutation.isPending} />
      </Modal>
      <Modal open={!!editLab} onClose={() => setEditLab(null)} title="Edytuj wynik" size="lg">
        {editLab && <LabForm defaultValues={editLab} onSubmit={d => updateMutation.mutate({ id: editLab.id, data: d })} submitting={updateMutation.isPending} />}
      </Modal>
      <ConfirmModal open={!!deleteLab} onClose={() => setDeleteLab(null)} onConfirm={() => deleteMutation.mutate(deleteLab.id)} title="Usun wynik" message="Czy na pewno chcesz usunac ten wynik badania?" danger />
    </div>
  );
}