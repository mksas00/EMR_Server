import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm, useFieldArray } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'react-toastify';
import { Plus, Trash2, Download, ArrowLeft, FileText, Search, X } from 'lucide-react';
import { prescriptionApi, patientApi, medicationApi } from '../../api';
import { LoadingOverlay } from '../../components/Spinner';
import { Pagination } from '../../components/Pagination';
import { Modal, ConfirmModal } from '../../components/Modal';

// ── Schemas ─────────────────────────────────────────────────────────────────

const itemSchema = z.object({
  medicationId: z.string().optional(),
  medicationName: z.string().optional(),
  dosageInfo: z.string().max(500).optional().or(z.literal('')),
  quantity: z.coerce.number().positive().int().optional().or(z.literal('')),
  unit: z.string().max(20).optional().or(z.literal('')),
});

const validYear = (val) => {
  if (!val) return true;
  const year = new Date(val).getFullYear();
  return year >= 1900 && year <= 2099;
};

const prescriptionSchema = z.object({
  issuedDate: z.string().min(1, 'Wymagane').refine(validYear, 'Nieprawidłowy rok (1900–2099)'),
  expirationDate: z.string().optional().or(z.literal('')).refine(v => !v || validYear(v), 'Nieprawidłowy rok (1900–2099)'),
  dosageInfo: z.string().max(1000).optional().or(z.literal('')),
  isRepeatable: z.boolean().optional(),
  items: z.array(itemSchema).optional(),
});

// ── Medication search field ──────────────────────────────────────────────────

function MedicationSearchField({ nameValue, onSelect, onClear }) {
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);

  const { data, isFetching } = useQuery({
    queryKey: ['med-search', query],
    queryFn: () =>
      query.trim().length >= 2
        ? medicationApi.search({ q: query.trim(), size: 8 }).then(r => r.data)
        : Promise.resolve({ content: [] }),
    enabled: query.trim().length >= 2,
  });

  if (nameValue) {
    return (
      <div className="flex items-center gap-2 input bg-gray-50">
        <span className="flex-1 text-sm text-gray-800">{nameValue}</span>
        <button type="button" onClick={onClear} className="text-gray-400 hover:text-red-500 flex-shrink-0">
          <X size={14} />
        </button>
      </div>
    );
  }

  return (
    <div className="relative">
      <div className="relative">
        <input
          value={query}
          onChange={e => { setQuery(e.target.value); setOpen(true); }}
          onFocus={() => setOpen(true)}
          onBlur={() => setTimeout(() => setOpen(false), 150)}
          className="input pr-8"
          placeholder="Wpisz min. 2 znaki nazwy leku..."
          autoComplete="off"
        />
        <Search size={14} className="absolute right-2.5 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
      </div>
      {open && query.trim().length >= 2 && (
        <div className="absolute z-20 w-full bg-white shadow-xl rounded-md border border-gray-200 mt-1 max-h-44 overflow-auto">
          {isFetching ? (
            <p className="p-3 text-xs text-gray-500">Wyszukiwanie...</p>
          ) : data?.content?.length === 0 ? (
            <p className="p-3 text-xs text-gray-500">Brak wyników dla „{query}"</p>
          ) : (
            data?.content?.map(med => (
              <button
                key={med.id}
                type="button"
                onMouseDown={e => { e.preventDefault(); onSelect(med); setQuery(''); setOpen(false); }}
                className="w-full text-left px-3 py-2 text-sm hover:bg-primary-50 border-b border-gray-50 last:border-0"
              >
                <span className="font-medium text-gray-800">{med.name}</span>
                {med.strength && <span className="text-gray-500 ml-1">{med.strength}</span>}
                {med.pharmaceuticalForm && (
                  <span className="text-gray-400 text-xs ml-1">· {med.pharmaceuticalForm}</span>
                )}
              </button>
            ))
          )}
        </div>
      )}
    </div>
  );
}

// ── Prescription form ────────────────────────────────────────────────────────

function PrescriptionForm({ defaultValues, onSubmit, submitting }) {
  const { register, control, handleSubmit, formState: { errors }, setValue, watch } = useForm({
    resolver: zodResolver(prescriptionSchema),
    defaultValues: defaultValues || { isRepeatable: false, items: [] },
  });

  const { fields, append, remove } = useFieldArray({ control, name: 'items' });
  const items = watch('items') || [];

  const addItem = () => {
    append({ medicationId: '', medicationName: '', dosageInfo: '', quantity: '', unit: '' });
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-5">
      {/* Dates */}
      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="label">Data wystawienia *</label>
          <input
            {...register('issuedDate')}
            type="date"
            min="1900-01-01"
            max="2099-12-31"
            className={`input ${errors.issuedDate ? 'input-error' : ''}`}
          />
          {errors.issuedDate && <p className="error-msg">{errors.issuedDate.message}</p>}
        </div>
        <div>
          <label className="label">Ważna do</label>
          <input {...register('expirationDate')} type="date" min="1900-01-01" max="2099-12-31" className="input" />
        </div>
      </div>

      {/* General notes */}
      <div>
        <label className="label">Ogólne zalecenia / notatki</label>
        <textarea
          {...register('dosageInfo')}
          rows={2}
          className="input"
          placeholder="Ogólne instrukcje dotyczące dawkowania..."
        />
      </div>

      {/* Repeatable */}
      <div className="flex items-center gap-2">
        <input
          {...register('isRepeatable')}
          type="checkbox"
          id="repeatable"
          className="rounded border-gray-300 text-primary-600"
        />
        <label htmlFor="repeatable" className="text-sm text-gray-700">Recepta powtarzalna</label>
      </div>

      {/* Medication items */}
      <div>
        <div className="flex items-center justify-between mb-2">
          <label className="label mb-0 font-semibold">Leki na recepcie</label>
          <button type="button" className="btn-secondary text-xs py-1" onClick={addItem}>
            <Plus size={13} /> Dodaj lek
          </button>
        </div>

        {fields.length === 0 ? (
          <div className="text-sm text-gray-400 text-center py-5 border-2 border-dashed border-gray-200 rounded-lg">
            Brak leków – kliknij „Dodaj lek" aby dodać pozycję
          </div>
        ) : (
          <div className="space-y-3">
            {fields.map((field, idx) => (
              <div key={field.id} className="border border-gray-200 rounded-lg p-3 space-y-3 bg-gray-50">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-gray-500 uppercase tracking-wide">
                    Lek #{idx + 1}
                  </span>
                  <button
                    type="button"
                    onClick={() => remove(idx)}
                    className="text-red-400 hover:text-red-600"
                    title="Usuń pozycję"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>

                {/* Medication search */}
                <div>
                  <label className="label text-xs">Nazwa leku (wyszukaj w bazie)</label>
                  <MedicationSearchField
                    nameValue={items[idx]?.medicationName}
                    onSelect={med => {
                      setValue(`items.${idx}.medicationId`, med.id);
                      setValue(`items.${idx}.medicationName`,
                        `${med.name}${med.strength ? ' ' + med.strength : ''}`);
                    }}
                    onClear={() => {
                      setValue(`items.${idx}.medicationId`, '');
                      setValue(`items.${idx}.medicationName`, '');
                    }}
                  />
                </div>

                {/* Quantity, unit */}
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="label text-xs">Ilość</label>
                    <input
                      {...register(`items.${idx}.quantity`)}
                      type="number"
                      min="1"
                      className="input"
                      placeholder="np. 2"
                    />
                  </div>
                  <div>
                    <label className="label text-xs">Jednostka</label>
                    <input
                      {...register(`items.${idx}.unit`)}
                      className="input"
                      placeholder="np. op."
                      maxLength={20}
                    />
                  </div>
                </div>

                {/* Per-item dosage */}
                <div>
                  <label className="label text-xs">Dawkowanie (pozycja)</label>
                  <input
                    {...register(`items.${idx}.dosageInfo`)}
                    className="input"
                    placeholder="np. 2×1 tabletka po jedzeniu"
                  />
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="flex justify-end gap-3 pt-2 border-t border-gray-100">
        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? 'Zapisywanie...' : 'Zapisz receptę'}
        </button>
      </div>
    </form>
  );
}

// ── Main page ────────────────────────────────────────────────────────────────

export default function PatientPrescriptionsPage() {
  const { patientId } = useParams();
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const [createOpen, setCreateOpen] = useState(false);
  const [deleteRx, setDeleteRx] = useState(null);

  const { data: patient } = useQuery({
    queryKey: ['patient', patientId],
    queryFn: () => patientApi.get(patientId).then(r => r.data),
    enabled: !!patientId,
  });

  const { data, isLoading } = useQuery({
    queryKey: ['prescriptions', 'patient', patientId, page],
    queryFn: () =>
      prescriptionApi
        .list({ patientId }, { page, size: 10, sort: 'issuedDate,desc' })
        .then(r => r.data),
    keepPreviousData: true,
    enabled: !!patientId,
  });

  const createMutation = useMutation({
    mutationFn: formData => {
      const body = {
        patientId,
        issuedDate: formData.issuedDate,
        expirationDate: formData.expirationDate || null,
        dosageInfo: formData.dosageInfo || null,
        isRepeatable: formData.isRepeatable || false,
        items: (formData.items || []).map(it => ({
          medicationId: it.medicationId || null,
          dosageInfo: it.dosageInfo || null,
          quantity: it.quantity ? parseInt(it.quantity) : null,
          unit: it.unit || null,
        })),
      };
      return prescriptionApi.create(body);
    },
    onSuccess: () => {
      qc.invalidateQueries(['prescriptions']);
      setCreateOpen(false);
      toast.success('Recepta wystawiona');
    },
    onError: e => toast.error(e.response?.data?.message || 'Błąd tworzenia recepty'),
  });

  const deleteMutation = useMutation({
    mutationFn: id => prescriptionApi.remove(id),
    onSuccess: () => {
      qc.invalidateQueries(['prescriptions']);
      setDeleteRx(null);
      toast.success('Recepta usunięta');
    },
    onError: e => toast.error(e.response?.data?.message || 'Błąd usuwania'),
  });

  const downloadPdf = async id => {
    try {
      const resp = await prescriptionApi.downloadPdf(id);
      const url = URL.createObjectURL(new Blob([resp.data], { type: 'application/pdf' }));
      const a = document.createElement('a');
      a.href = url;
      a.download = `recepta-${id}.pdf`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      toast.error('Nie można pobrać PDF');
    }
  };

  const patientName = patient
    ? `${patient.firstName || ''} ${patient.lastName || ''}`.trim()
    : '';

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Link to={`/patients/${patientId}`} className="btn-ghost p-2 text-gray-500" title="Wróć do pacjenta">
          <ArrowLeft size={18} />
        </Link>
        <div className="flex-1">
          <h1 className="page-title">Recepty</h1>
          {patientName && (
            <p className="text-sm text-gray-500">Pacjent: {patientName}</p>
          )}
        </div>
        <button className="btn-primary" onClick={() => setCreateOpen(true)}>
          <Plus size={16} /> Nowa recepta
        </button>
      </div>

      {/* Table */}
      <div className="card p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr>
                <th className="table-header">Data wystawienia</th>
                <th className="table-header">Ważna do</th>
                <th className="table-header">Leki</th>
                <th className="table-header">Zalecenia</th>
                <th className="table-header">Powtarzalna</th>
                <th className="table-header text-right">Akcje</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {isLoading ? (
                <tr>
                  <td colSpan={6}>
                    <LoadingOverlay />
                  </td>
                </tr>
              ) : data?.content?.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center py-12 text-gray-400">
                    <FileText size={32} className="mx-auto mb-2 opacity-40" />
                    <p className="text-sm">Brak recept dla tego pacjenta</p>
                  </td>
                </tr>
              ) : (
                data?.content?.map(rx => (
                  <tr key={rx.id} className="hover:bg-gray-50 transition-colors">
                    <td className="table-cell font-medium">{rx.issuedDate || '—'}</td>
                    <td className="table-cell">{rx.expirationDate || '—'}</td>
                    <td className="table-cell">
                      {rx.items?.length > 0 ? (
                        <div className="space-y-0.5">
                          {rx.items.map((item, i) => (
                            <p key={i} className="text-xs text-gray-700">
                              {item.medicationName || `Lek #${i + 1}`}
                              {item.quantity
                                ? ` × ${item.quantity}${item.unit ? ' ' + item.unit : ''}`
                                : ''}
                            </p>
                          ))}
                        </div>
                      ) : (
                        <span className="text-gray-400 text-xs">—</span>
                      )}
                    </td>
                    <td className="table-cell max-w-xs">
                      <p className="truncate text-xs text-gray-600">{rx.dosageInfo || '—'}</p>
                    </td>
                    <td className="table-cell">
                      <span className={`badge ${rx.isRepeatable ? 'badge-green' : 'badge-gray'}`}>
                        {rx.isRepeatable ? 'Tak' : 'Nie'}
                      </span>
                    </td>
                    <td className="table-cell">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          className="btn-ghost p-1.5 text-gray-500 hover:text-primary-600"
                          title="Pobierz PDF"
                          onClick={() => downloadPdf(rx.id)}
                        >
                          <Download size={15} />
                        </button>
                        <button
                          className="btn-ghost p-1.5 text-gray-500 hover:text-red-600"
                          title="Usuń"
                          onClick={() => setDeleteRx(rx)}
                        >
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

      {/* Create modal */}
      <Modal open={createOpen} onClose={() => setCreateOpen(false)} title="Nowa recepta" size="lg">
        <PrescriptionForm
          onSubmit={d => createMutation.mutate(d)}
          submitting={createMutation.isPending}
        />
      </Modal>

      {/* Delete confirm */}
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

