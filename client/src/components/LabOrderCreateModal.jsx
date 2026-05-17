import { useEffect, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm, useFieldArray } from 'react-hook-form';
import { toast } from 'react-toastify';
import { Plus, Trash2, Beaker } from 'lucide-react';
import { labOrderApi, labPanelApi } from '../api';
import { Modal } from './Modal';

/**
 * Modal tworzenia zlecenia laboratoryjnego.
 *
 * Props:
 *  - open: boolean
 *  - onClose: () => void
 *  - initialPatientId?: string  – pre-wypełnia pole pacjenta (np. z karty pacjenta)
 *  - patientName?: string       – wyświetlana nazwa zamiast UUID
 */
export default function LabOrderCreateModal({ open, onClose, initialPatientId, patientName }) {
  const qc = useQueryClient();
  const [selectedPanels, setSelectedPanels] = useState({});

  const { register, handleSubmit, control, reset, formState: { errors } } = useForm({
    defaultValues: { patientId: initialPatientId || '', priority: 'routine', notes: '', items: [] },
  });
  const { fields, append, remove } = useFieldArray({ control, name: 'items' });

  // Re-initialize form when modal opens
  useEffect(() => {
    if (open) {
      reset({ patientId: initialPatientId || '', priority: 'routine', notes: '', items: [] });
      setSelectedPanels({});
    }
  }, [open, initialPatientId, reset]);

  const { data: panels = [] } = useQuery({
    queryKey: ['labPanels'],
    queryFn: () => labPanelApi.list().then(r => r.data),
    enabled: open,
    staleTime: 5 * 60_000,
  });

  const createMutation = useMutation({
    mutationFn: (data) => labOrderApi.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['labOrders'] });
      toast.success('Zlecenie laboratoryjne utworzone');
      onClose();
    },
    onError: (e) => toast.error(e.response?.data?.message || 'Błąd tworzenia zlecenia'),
  });

  const togglePanel = (id) => setSelectedPanels(prev => ({ ...prev, [id]: !prev[id] }));

  const onSubmit = (form) => {
    const panelIds = Object.entries(selectedPanels).filter(([, v]) => v).map(([k]) => k);
    if (panelIds.length === 0 && (!form.items || form.items.length === 0)) {
      toast.error('Wybierz panel lub dodaj co najmniej jeden test');
      return;
    }
    createMutation.mutate({
      patientId: form.patientId,
      priority: form.priority,
      notes: form.notes || null,
      panelIds,
      items: (form.items || []).map(it => ({
        testCode: it.testCode,
        testName: it.testName,
        unit: it.unit || null,
        refRangeText: it.refRangeText || null,
      })),
    });
  };

  return (
    <Modal open={open} onClose={onClose} title="Nowe zlecenie laboratoryjne" size="xl">
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5" noValidate>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="md:col-span-2">
            {initialPatientId ? (
              <div>
                <label className="label">Pacjent</label>
                <p className="text-sm font-medium text-gray-800 py-2 px-3 bg-gray-50 rounded-lg">
                  {patientName || initialPatientId}
                </p>
                <input type="hidden" {...register('patientId')} />
              </div>
            ) : (
              <div>
                <label className="label">ID pacjenta (UUID) *</label>
                <input
                  {...register('patientId', { required: 'Wymagane', pattern: /^[0-9a-f-]{36}$/i })}
                  className={`input font-mono ${errors.patientId ? 'input-error' : ''}`}
                  placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" />
                {errors.patientId && <p className="error-msg">{errors.patientId.message}</p>}
              </div>
            )}
          </div>
          <div>
            <label className="label">Priorytet</label>
            <select {...register('priority')} className="input">
              <option value="routine">Rutynowy</option>
              <option value="urgent">Pilny</option>
              <option value="stat">CITO</option>
            </select>
          </div>
        </div>

        <div>
          <label className="label">Notatka kliniczna (opcjonalna)</label>
          <textarea {...register('notes')} className="input" rows={2} maxLength={2000} />
        </div>

        {/* Panels */}
        <div>
          <p className="label mb-2 flex items-center gap-2">
            <Beaker size={16} /> Panele predefiniowane
          </p>
          {panels.length === 0 ? (
            <p className="text-xs text-gray-400">Brak paneli</p>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-2 max-h-48 overflow-y-auto">
              {panels.map(p => {
                const selected = !!selectedPanels[p.id];
                return (
                  <label key={p.id}
                    className={`flex items-start gap-2 p-3 rounded-lg border cursor-pointer transition-colors ${selected ? 'border-primary-500 bg-primary-50' : 'border-gray-200 hover:border-gray-300'}`}>
                    <input type="checkbox" checked={selected} onChange={() => togglePanel(p.id)} className="mt-0.5" />
                    <div className="flex-1">
                      <p className="text-sm font-medium">{p.name}</p>
                      <p className="text-xs text-gray-500">{p.description}</p>
                      <p className="text-xs text-gray-400 mt-0.5">{p.items?.length ?? 0} testów</p>
                    </div>
                  </label>
                );
              })}
            </div>
          )}
        </div>

        {/* Single tests */}
        <div>
          <div className="flex items-center justify-between mb-2">
            <p className="label">Pojedyncze testy (opcjonalne)</p>
            <button type="button" className="btn-ghost text-xs"
              onClick={() => append({ testCode: '', testName: '', unit: '', refRangeText: '' })}>
              <Plus size={14} /> Dodaj test
            </button>
          </div>
          {fields.map((f, idx) => (
            <div key={f.id} className="grid grid-cols-12 gap-2 mb-2">
              <input {...register(`items.${idx}.testCode`, { required: true })}
                placeholder="Kod (np. CRP)" className="input col-span-2 text-sm" maxLength={40} />
              <input {...register(`items.${idx}.testName`, { required: true })}
                placeholder="Nazwa badania" className="input col-span-5 text-sm" maxLength={200} />
              <input {...register(`items.${idx}.unit`)}
                placeholder="Jednostka" className="input col-span-2 text-sm" maxLength={20} />
              <input {...register(`items.${idx}.refRangeText`)}
                placeholder="Zakres referencyjny" className="input col-span-2 text-sm" />
              <button type="button" onClick={() => remove(idx)}
                className="btn-ghost col-span-1 text-red-500 hover:text-red-700"><Trash2 size={14} /></button>
            </div>
          ))}
        </div>

        <div className="flex justify-end gap-3 pt-2 border-t border-gray-100">
          <button type="button" className="btn-secondary" onClick={onClose}>Anuluj</button>
          <button type="submit" className="btn-primary" disabled={createMutation.isPending}>
            {createMutation.isPending ? 'Tworzenie...' : 'Utwórz zlecenie'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

