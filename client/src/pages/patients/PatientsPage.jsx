import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'react-toastify';
import { Plus, Search, Edit2, Trash2, Eye, User } from 'lucide-react';
import { patientApi } from '../../api';
import { LoadingOverlay } from '../../components/Spinner';
import { Pagination } from '../../components/Pagination';
import { Modal, ConfirmModal } from '../../components/Modal';
import { Link } from 'react-router-dom';
import { format } from 'date-fns';

const patientSchema = z.object({
  firstName: z.string().min(1, 'Wymagane').max(100).regex(/^[a-zA-ZąćęłńóśźżĄĆĘŁŃÓŚŹŻ\s-]+$/, 'Tylko litery'),
  lastName:  z.string().min(1, 'Wymagane').max(100).regex(/^[a-zA-ZąćęłńóśźżĄĆĘŁŃÓŚŹŻ\s-]+$/, 'Tylko litery'),
  pesel:     z.string().length(11, 'PESEL musi mieć 11 cyfr').regex(/^\d{11}$/, 'Tylko cyfry').optional().or(z.literal('')),
  dateOfBirth: z.string().min(1, 'Wymagane'),
  gender:    z.enum(['M', 'F', 'OTHER', '']).optional(),
  address:   z.string().max(500).optional().or(z.literal('')),
});

function PatientForm({ defaultValues, onSubmit, submitting }) {
  const { register, handleSubmit, formState: { errors } } = useForm({
    resolver: zodResolver(patientSchema),
    defaultValues: defaultValues || { gender: '' },
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="label">Imię *</label>
          <input {...register('firstName')} className={`input ${errors.firstName ? 'input-error' : ''}`} />
          {errors.firstName && <p className="error-msg">{errors.firstName.message}</p>}
        </div>
        <div>
          <label className="label">Nazwisko *</label>
          <input {...register('lastName')} className={`input ${errors.lastName ? 'input-error' : ''}`} />
          {errors.lastName && <p className="error-msg">{errors.lastName.message}</p>}
        </div>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="label">PESEL</label>
          <input {...register('pesel')} className={`input ${errors.pesel ? 'input-error' : ''}`} maxLength={11} inputMode="numeric" />
          {errors.pesel && <p className="error-msg">{errors.pesel.message}</p>}
        </div>
        <div>
          <label className="label">Data urodzenia *</label>
          <input {...register('dateOfBirth')} type="date" className={`input ${errors.dateOfBirth ? 'input-error' : ''}`} />
          {errors.dateOfBirth && <p className="error-msg">{errors.dateOfBirth.message}</p>}
        </div>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="label">Płeć</label>
          <select {...register('gender')} className="input">
            <option value="">— wybierz —</option>
            <option value="M">Mężczyzna</option>
            <option value="F">Kobieta</option>
            <option value="OTHER">Inne</option>
          </select>
        </div>
        <div>
          <label className="label">Adres</label>
          <input {...register('address')} className="input" placeholder="ul. Przykładowa 1, Warszawa" />
        </div>
      </div>
      <div className="flex justify-end gap-3 pt-2">
        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? 'Zapisywanie...' : 'Zapisz'}
        </button>
      </div>
    </form>
  );
}

export default function PatientsPage() {
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState({ firstName: '', lastName: '', pesel: '' });
  const [filters, setFilters] = useState({});
  const [createOpen, setCreateOpen] = useState(false);
  const [editPatient, setEditPatient] = useState(null);
  const [deletePatient, setDeletePatient] = useState(null);

  const { data, isLoading } = useQuery({
    queryKey: ['patients', filters, page],
    queryFn: () => patientApi.list(filters, { page, size: 10, sort: 'createdAt,desc' }).then(r => r.data),
    keepPreviousData: true,
  });

  const createMutation = useMutation({
    mutationFn: (data) => patientApi.create(data),
    onSuccess: () => { qc.invalidateQueries(['patients']); setCreateOpen(false); toast.success('Pacjent dodany pomyślnie'); },
    onError: (e) => toast.error(e.response?.data?.message || 'Błąd podczas tworzenia pacjenta'),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }) => patientApi.update(id, data),
    onSuccess: () => { qc.invalidateQueries(['patients']); setEditPatient(null); toast.success('Dane pacjenta zaktualizowane'); },
    onError: (e) => toast.error(e.response?.data?.message || 'Błąd podczas aktualizacji'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id) => patientApi.remove(id),
    onSuccess: () => { qc.invalidateQueries(['patients']); setDeletePatient(null); toast.success('Pacjent usunięty'); },
    onError: (e) => toast.error(e.response?.data?.message || 'Błąd podczas usuwania'),
  });

  const applySearch = () => {
    setFilters({ ...search });
    setPage(0);
  };

  const genderLabel = { M: 'Mężczyzna', F: 'Kobieta', OTHER: 'Inne' };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="page-title">Pacjenci</h1>
          <p className="text-sm text-gray-500 mt-1">Zarządzanie danymi pacjentów</p>
        </div>
        <button className="btn-primary" onClick={() => setCreateOpen(true)}>
          <Plus size={16} /> Nowy pacjent
        </button>
      </div>

      {/* Search bar */}
      <div className="card py-4">
        <div className="flex flex-wrap gap-3">
          <input
            className="input w-40"
            placeholder="Imię"
            value={search.firstName}
            onChange={e => setSearch(s => ({ ...s, firstName: e.target.value }))}
          />
          <input
            className="input w-44"
            placeholder="Nazwisko"
            value={search.lastName}
            onChange={e => setSearch(s => ({ ...s, lastName: e.target.value }))}
          />
          <input
            className="input w-36"
            placeholder="PESEL"
            maxLength={11}
            inputMode="numeric"
            value={search.pesel}
            onChange={e => setSearch(s => ({ ...s, pesel: e.target.value.replace(/\D/g, '') }))}
          />
          <button className="btn-primary gap-2" onClick={applySearch}>
            <Search size={15} /> Szukaj
          </button>
          <button className="btn-secondary" onClick={() => { setSearch({ firstName: '', lastName: '', pesel: '' }); setFilters({}); setPage(0); }}>
            Wyczyść
          </button>
        </div>
      </div>

      {/* Table */}
      <div className="card p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr>
                <th className="table-header">Pacjent</th>
                <th className="table-header">PESEL</th>
                <th className="table-header">Data ur.</th>
                <th className="table-header">Płeć</th>
                <th className="table-header text-right">Akcje</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {isLoading ? (
                <tr><td colSpan={5}><LoadingOverlay /></td></tr>
              ) : data?.content?.length === 0 ? (
                <tr>
                  <td colSpan={5} className="text-center py-12 text-gray-400">
                    <User size={32} className="mx-auto mb-2 opacity-40" />
                    <p className="text-sm">Brak pacjentów</p>
                  </td>
                </tr>
              ) : (
                data?.content?.map((p) => (
                  <tr key={p.id} className="hover:bg-gray-50 transition-colors">
                    <td className="table-cell">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-primary-100 flex items-center justify-center text-primary-700 text-sm font-semibold">
                          {(p.firstName?.[0] || '?').toUpperCase()}
                        </div>
                        <div>
                          <p className="font-medium text-gray-800">{p.firstName} {p.lastName}</p>
                        </div>
                      </div>
                    </td>
                    <td className="table-cell font-mono">{p.pesel || '—'}</td>
                    <td className="table-cell">{p.dateOfBirth || '—'}</td>
                    <td className="table-cell">{genderLabel[p.gender] || p.gender || '—'}</td>
                    <td className="table-cell">
                      <div className="flex items-center justify-end gap-2">
                        <Link to={`/patients/${p.id}`} className="btn-ghost p-1.5 text-gray-500 hover:text-primary-600" title="Podgląd">
                          <Eye size={16} />
                        </Link>
                        <button className="btn-ghost p-1.5 text-gray-500 hover:text-primary-600" title="Edytuj" onClick={() => setEditPatient(p)}>
                          <Edit2 size={16} />
                        </button>
                        <button className="btn-ghost p-1.5 text-gray-500 hover:text-red-600" title="Usuń" onClick={() => setDeletePatient(p)}>
                          <Trash2 size={16} />
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

      {/* Create modal */}
      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Nowy pacjent" size="lg">
        <PatientForm
          onSubmit={(d) => createMutation.mutate(d)}
          submitting={createMutation.isPending}
        />
      </Modal>

      {/* Edit modal */}
      <Modal open={!!editPatient} onClose={() => setEditPatient(null)} title="Edytuj pacjenta" size="lg">
        {editPatient && (
          <PatientForm
            defaultValues={{ ...editPatient, gender: editPatient.gender || '' }}
            onSubmit={(d) => updateMutation.mutate({ id: editPatient.id, data: d })}
            submitting={updateMutation.isPending}
          />
        )}
      </Modal>

      {/* Delete confirm */}
      <ConfirmModal
        open={!!deletePatient}
        onClose={() => setDeletePatient(null)}
        onConfirm={() => deleteMutation.mutate(deletePatient.id)}
        title="Usuń pacjenta"
        message={`Czy na pewno chcesz usunąć pacjenta ${deletePatient?.firstName} ${deletePatient?.lastName}? Tej operacji nie można cofnąć.`}
        danger
      />
    </div>
  );
}
