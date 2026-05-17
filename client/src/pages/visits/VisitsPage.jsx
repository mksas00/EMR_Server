import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'react-toastify';
import { Plus, Search, Edit2, Trash2, Download, Calendar, X, Check } from 'lucide-react';
import { visitApi, patientApi } from '../../api';
import { LoadingOverlay } from '../../components/Spinner';
import { Pagination } from '../../components/Pagination';
import { Modal, ConfirmModal } from '../../components/Modal';
import { format } from 'date-fns';
import { pl } from 'date-fns/locale';

const visitSchema = z.object({
  patientId: z.string().uuid('Nieprawidłowe ID pacjenta'),
  visitDate: z.string().min(1, 'Wymagane'),
  endDate: z.string().optional().or(z.literal('')),
  visitType: z.string().min(1, 'Wymagane').max(50),
  reason: z.string().max(500).optional().or(z.literal('')),
  diagnosis: z.string().max(500).optional().or(z.literal('')),
  notes: z.string().max(2000).optional().or(z.literal('')),
  status: z.enum(['PLANNED', 'CONFIRMED', 'COMPLETED', 'CANCELED', '']).optional(),
  isConfidential: z.boolean().optional(),
});

const statusColor = {
  PLANNED: 'badge-blue', CONFIRMED: 'badge-green',
  COMPLETED: 'badge-gray', CANCELED: 'badge-red',
};
const statusLabel = {
  PLANNED: 'Zaplanowana', CONFIRMED: 'Potwierdzona',
  COMPLETED: 'Zakończona', CANCELED: 'Anulowana',
};

function VisitForm({ defaultValues, onSubmit, submitting, lockPatient }) {
  const { register, handleSubmit, formState: { errors }, setValue, watch } = useForm({
    resolver: zodResolver(visitSchema),
    defaultValues: defaultValues || { status: 'PLANNED', isConfidential: false },
  });

  // Hidden field that actually gets submitted (UUID), driven by patient search.
  register('patientId');
  const patientId = watch('patientId');

  // Local UI state for the PESEL search box.
  const [pesel, setPesel] = useState(defaultValues?._pesel || '');
  const [selectedPatient, setSelectedPatient] = useState(defaultValues?._patient || null);

  // Search backend only when PESEL is full 11 digits and no patient picked yet.
  const { data: searchData, isFetching: searching } = useQuery({
    queryKey: ['visit-patient-search', pesel],
    queryFn: () => patientApi.list({ pesel }, { page: 0, size: 5 }).then(r => r.data),
    enabled: pesel.length === 11 && !selectedPatient && !lockPatient,
    staleTime: 30_000,
  });

  const match = searchData?.content?.[0];
  const notFound = pesel.length === 11 && !selectedPatient && !searching && !match;

  const handleSelect = (p) => {
    setSelectedPatient(p);
    setValue('patientId', p.id, { shouldValidate: true });
  };

  const handleClear = () => {
    setSelectedPatient(null);
    setPesel('');
    setValue('patientId', '', { shouldValidate: true });
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
      {/* Patient picker (PESEL → autocomplete) */}
      <div>
        <label className="label">Pacjent (PESEL) *</label>
        {selectedPatient || lockPatient ? (
          <div className="flex items-center gap-2 input bg-primary-50 border-primary-200">
            <Check size={16} className="text-primary-600 flex-shrink-0" />
            <div className="flex-1 text-sm">
              <span className="font-mono font-medium text-gray-800">
                {selectedPatient?.pesel || defaultValues?._pesel || '—'}
              </span>
              <span className="mx-2 text-gray-300">·</span>
              <span className="text-gray-700">
                {selectedPatient
                  ? `${selectedPatient.firstName || ''} ${selectedPatient.lastName || ''}`.trim()
                  : `${defaultValues?._firstName || ''} ${defaultValues?._lastName || ''}`.trim() || '—'}
              </span>
            </div>
            {!lockPatient && (
              <button type="button" onClick={handleClear}
                      className="text-gray-400 hover:text-red-500 flex-shrink-0" title="Zmień pacjenta">
                <X size={15} />
              </button>
            )}
          </div>
        ) : (
          <div className="relative">
            <input
              value={pesel}
              onChange={e => setPesel(e.target.value.replace(/\D/g, '').slice(0, 11))}
              inputMode="numeric"
              maxLength={11}
              autoComplete="off"
              placeholder="Wpisz 11-cyfrowy PESEL pacjenta"
              className={`input font-mono pr-8 ${errors.patientId ? 'input-error' : ''}`}
            />
            <Search size={14} className="absolute right-2.5 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
            {/* Dropdown with single match / not-found / searching state */}
            {pesel.length === 11 && (
              <div className="absolute z-20 w-full bg-white shadow-xl rounded-md border border-gray-200 mt-1 overflow-hidden">
                {searching ? (
                  <p className="p-3 text-xs text-gray-500">Wyszukiwanie...</p>
                ) : match ? (
                  <button
                    type="button"
                    onMouseDown={e => { e.preventDefault(); handleSelect(match); }}
                    className="w-full text-left px-3 py-2 text-sm hover:bg-primary-50"
                  >
                    <div className="font-medium text-gray-800">
                      {match.firstName} {match.lastName}
                    </div>
                    <div className="text-xs text-gray-500 font-mono">
                      PESEL: {match.pesel}
                      {match.dateOfBirth && <span className="ml-2">· ur. {match.dateOfBirth}</span>}
                    </div>
                  </button>
                ) : notFound ? (
                  <p className="p-3 text-xs text-red-600">Nie znaleziono pacjenta o tym numerze PESEL</p>
                ) : null}
              </div>
            )}
          </div>
        )}
        {errors.patientId && !selectedPatient && !lockPatient && (
          <p className="error-msg">Wybierz pacjenta z listy</p>
        )}
        {!selectedPatient && !lockPatient && pesel.length > 0 && pesel.length < 11 && (
          <p className="text-xs text-gray-400 mt-1">Wpisz pełny 11-cyfrowy PESEL aby wyszukać pacjenta</p>
        )}
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="label">Data wizyty *</label>
          <input {...register('visitDate')} type="datetime-local" className={`input ${errors.visitDate ? 'input-error' : ''}`} />
          {errors.visitDate && <p className="error-msg">{errors.visitDate.message}</p>}
        </div>
        <div>
          <label className="label">Koniec wizyty</label>
          <input {...register('endDate')} type="datetime-local" className="input" />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="label">Typ wizyty *</label>
          <select {...register('visitType')} className={`input ${errors.visitType ? 'input-error' : ''}`}>
            <option value="">— wybierz —</option>
            <option value="APPOINTMENT">Wizyta</option>
            <option value="FOLLOW_UP">Kontrolna</option>
            <option value="EMERGENCY">Pilna</option>
            <option value="CONSULTATION">Konsultacja</option>
            <option value="TELEMEDICAL">Telemedyczna</option>
          </select>
          {errors.visitType && <p className="error-msg">{errors.visitType.message}</p>}
        </div>
        <div>
          <label className="label">Status</label>
          <select {...register('status')} className="input">
            <option value="PLANNED">Zaplanowana</option>
            <option value="CONFIRMED">Potwierdzona</option>
            <option value="COMPLETED">Zakończona</option>
            <option value="CANCELED">Anulowana</option>
          </select>
        </div>
      </div>
      <div>
        <label className="label">Powód wizyty</label>
        <textarea {...register('reason')} rows={2} className="input" />
      </div>
      <div>
        <label className="label">Diagnoza</label>
        <textarea {...register('diagnosis')} rows={2} className="input" />
      </div>
      <div>
        <label className="label">Notatki</label>
        <textarea {...register('notes')} rows={3} className="input" />
      </div>
      <div className="flex items-center gap-2">
        <input {...register('isConfidential')} type="checkbox" className="rounded border-gray-300 text-primary-600" id="confidential" />
        <label htmlFor="confidential" className="text-sm text-gray-700">Wizyta poufna</label>
      </div>
      <div className="flex justify-end gap-3 pt-2">
        <button type="submit" className="btn-primary" disabled={submitting || !patientId}>{submitting ? 'Zapisywanie...' : 'Zapisz'}</button>
      </div>
    </form>
  );
}

export default function VisitsPage() {
  const qc = useQueryClient();
  const [searchParams] = useSearchParams();
  const [page, setPage] = useState(0);
  // Support both patientId (from PatientDetail link) and pesel (user search)
  const initialPatientId = searchParams.get('patientId') || '';
  const [filters, setFilters] = useState({ pesel: '', status: '', patientId: initialPatientId });
  const [activeFilters, setActiveFilters] = useState({ patientId: initialPatientId });
  const [createOpen, setCreateOpen] = useState(false);
  const [editVisit, setEditVisit] = useState(null);
  const [deleteVisit, setDeleteVisit] = useState(null);

  const { data, isLoading } = useQuery({
    queryKey: ['visits', 'list', activeFilters, page],
    queryFn: () => visitApi.list(activeFilters, { page, size: 10, sort: 'visitDate,desc' }).then(r => r.data),
    keepPreviousData: true,
  });

  const createMutation = useMutation({
    mutationFn: (d) => visitApi.create(d.patientId, { ...d }),
    onSuccess: () => { qc.invalidateQueries(['visits']); setCreateOpen(false); toast.success('Wizyta utworzona'); },
    onError: (e) => toast.error(e.response?.data?.message || 'Błąd tworzenia wizyty'),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => visitApi.update(id, data),
    onSuccess: () => { qc.invalidateQueries(['visits']); setEditVisit(null); toast.success('Wizyta zaktualizowana'); },
    onError: (e) => toast.error(e.response?.data?.message || 'Błąd aktualizacji'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id) => visitApi.remove(id),
    onSuccess: () => { qc.invalidateQueries(['visits']); setDeleteVisit(null); toast.success('Wizyta usunięta'); },
    onError: (e) => toast.error(e.response?.data?.message || 'Błąd usuwania'),
  });

  const downloadIcs = async (id) => {
    try {
      const resp = await visitApi.downloadIcs(id);
      const url = URL.createObjectURL(new Blob([resp.data]));
      const a = document.createElement('a'); a.href = url; a.download = `visit-${id}.ics`; a.click();
      URL.revokeObjectURL(url);
    } catch { toast.error('Nie można pobrać pliku ICS'); }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="page-title">Wizyty</h1>
          <p className="text-sm text-gray-500 mt-1">Historia i planowanie wizyt</p>
        </div>
        <button className="btn-primary" onClick={() => setCreateOpen(true)}>
          <Plus size={16} /> Nowa wizyta
        </button>
      </div>

      <div className="card py-4">
        <div className="flex flex-wrap gap-3">
          <input className="input w-36" placeholder="PESEL" maxLength={11} inputMode="numeric"
            value={filters.pesel}
            onChange={e => setFilters(f => ({ ...f, pesel: e.target.value.replace(/\D/g, ''), patientId: '' }))} />
          <select className="input w-40" value={filters.status}
            onChange={e => setFilters(f => ({ ...f, status: e.target.value }))}>
            <option value="">Wszystkie statusy</option>
            <option value="PLANNED">Zaplanowane</option>
            <option value="CONFIRMED">Potwierdzone</option>
            <option value="COMPLETED">Zakończone</option>
            <option value="CANCELED">Anulowane</option>
          </select>
          <button className="btn-primary" onClick={() => {
            const active = {};
            if (filters.pesel) active.pesel = filters.pesel;
            else if (filters.patientId) active.patientId = filters.patientId;
            if (filters.status) active.status = filters.status;
            setActiveFilters(active);
            setPage(0);
          }}>
            <Search size={15} /> Szukaj
          </button>
          <button className="btn-secondary" onClick={() => { setFilters({ pesel: '', status: '', patientId: '' }); setActiveFilters({}); setPage(0); }}>
            Wyczyść
          </button>
        </div>
      </div>

      <div className="card p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr>
                <th className="table-header">Data</th>
                <th className="table-header">Typ</th>
                <th className="table-header">Powód / Diagnoza</th>
                <th className="table-header">Status</th>
                <th className="table-header text-right">Akcje</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {isLoading ? (
                <tr><td colSpan={5}><LoadingOverlay /></td></tr>
              ) : data?.content?.length === 0 ? (
                <tr>
                  <td colSpan={5} className="text-center py-12 text-gray-400">
                    <Calendar size={32} className="mx-auto mb-2 opacity-40" />
                    <p className="text-sm">Brak wizyt</p>
                  </td>
                </tr>
              ) : (
                data?.content?.map((v) => (
                  <tr key={v.id} className="hover:bg-gray-50 transition-colors">
                    <td className="table-cell">
                      <p className="font-medium">{v.visitDate ? format(new Date(v.visitDate), 'dd.MM.yyyy', { locale: pl }) : '—'}</p>
                      <p className="text-xs text-gray-400">{v.visitDate ? format(new Date(v.visitDate), 'HH:mm') : ''}</p>
                    </td>
                    <td className="table-cell">{v.visitType || '—'}</td>
                    <td className="table-cell max-w-xs">
                      {v.reason && <p className="truncate text-xs text-gray-600">{v.reason}</p>}
                      {v.diagnosis && <p className="truncate text-xs text-gray-500 italic">{v.diagnosis}</p>}
                    </td>
                    <td className="table-cell">
                      <span className={`badge ${statusColor[v.status] || 'badge-gray'}`}>
                        {statusLabel[v.status] || v.status}
                      </span>
                    </td>
                    <td className="table-cell">
                      <div className="flex items-center justify-end gap-2">
                        <button className="btn-ghost p-1.5 text-gray-500 hover:text-primary-600" title="Pobierz ICS" onClick={() => downloadIcs(v.id)}>
                          <Download size={15} />
                        </button>
                        <button className="btn-ghost p-1.5 text-gray-500 hover:text-primary-600" title="Edytuj" onClick={() => setEditVisit(v)}>
                          <Edit2 size={15} />
                        </button>
                        <button className="btn-ghost p-1.5 text-gray-500 hover:text-red-600" title="Usuń" onClick={() => setDeleteVisit(v)}>
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

      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Nowa wizyta" size="lg">
        <VisitForm onSubmit={(d) => {
          const patientId = d.patientId;
          const { patientId: _, ...rest } = d;
          createMutation.mutate({ ...rest, patientId });
        }} submitting={createMutation.isPending} />
      </Modal>

      <Modal open={!!editVisit} onClose={() => setEditVisit(null)} title="Edytuj wizytę" size="lg">
        {editVisit && (
          <VisitForm
            lockPatient
            defaultValues={{
              ...editVisit,
              visitDate: editVisit.visitDate ? new Date(editVisit.visitDate).toISOString().slice(0, 16) : '',
              endDate: editVisit.endDate ? new Date(editVisit.endDate).toISOString().slice(0, 16) : '',
              patientId: editVisit.patientId || '',
              // Display-only helpers for the locked patient pill in VisitForm.
              _firstName: editVisit.patientFullName || '',
              _lastName: '',
              _pesel: editVisit.patientId ? editVisit.patientId.slice(0, 8) + '…' : '—',
            }}
            onSubmit={(d) => { const { patientId, ...rest } = d; updateMutation.mutate({ id: editVisit.id, data: rest }); }}
            submitting={updateMutation.isPending}
          />
        )}
      </Modal>

      <ConfirmModal
        open={!!deleteVisit}
        onClose={() => setDeleteVisit(null)}
        onConfirm={() => deleteMutation.mutate(deleteVisit.id)}
        title="Usuń wizytę"
        message="Czy na pewno chcesz usunąć tę wizytę?"
        danger
      />
    </div>
  );
}
