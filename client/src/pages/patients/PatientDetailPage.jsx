import { useParams, Link } from 'react-router-dom';
import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'react-toastify';
import { ArrowLeft, Calendar, FileText, FlaskConical, AlertTriangle, Heart, Phone, Mail, Plus, ClipboardList } from 'lucide-react';
import { patientApi, allergyApi, visitApi, prescriptionApi, labResultApi, chronicDiseaseApi, labOrderApi } from '../../api';
import { LoadingOverlay } from '../../components/Spinner';
import ConsentPanel from '../../components/ConsentPanel';
import { Modal } from '../../components/Modal';
import LabOrderCreateModal from '../../components/LabOrderCreateModal';
import { format } from 'date-fns';
import { pl } from 'date-fns/locale';
import { useAuth } from '../../contexts/AuthContext';

function InfoRow({ label, value }) {
  return (
    <div className="flex justify-between py-2 border-b border-gray-50 last:border-0">
      <span className="text-sm text-gray-500">{label}</span>
      <span className="text-sm font-medium text-gray-800">{value || '—'}</span>
    </div>
  );
}

const severityColor = {
  LOW: 'badge-green', MILD: 'badge-yellow', MODERATE: 'badge-yellow',
  SEVERE: 'badge-red', HIGH: 'badge-red', low: 'badge-green', moderate: 'badge-yellow', high: 'badge-red',
};

// ── Allergy form ──────────────────────────────────────────────────────────
const allergySchema = z.object({
  allergen: z.string().min(1, 'Wymagane').max(200),
  reaction: z.string().max(500).optional().or(z.literal('')),
  severity: z.enum(['low', 'moderate', 'high', '']).optional(),
});

function AddAllergyModal({ open, onClose, patientId }) {
  const qc = useQueryClient();
  const { register, handleSubmit, reset, formState: { errors } } = useForm({
    resolver: zodResolver(allergySchema),
    defaultValues: { severity: 'low' },
  });

  const mutation = useMutation({
    mutationFn: (data) => allergyApi.create({ ...data, patientId }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['allergies', patientId] });
      toast.success('Alergia dodana');
      reset();
      onClose();
    },
    onError: (e) => toast.error(e.response?.data?.message || 'Błąd dodawania alergii'),
  });

  return (
    <Modal open={open} onClose={onClose} title="Dodaj alergię" size="sm">
      <form onSubmit={handleSubmit((d) => mutation.mutate(d))} noValidate className="space-y-4">
        <div>
          <label className="label">Alergen *</label>
          <input {...register('allergen')} className={`input ${errors.allergen ? 'input-error' : ''}`}
            placeholder="np. Penicylina, Orzechy, Pyłki..." />
          {errors.allergen && <p className="error-msg">{errors.allergen.message}</p>}
        </div>
        <div>
          <label className="label">Reakcja</label>
          <input {...register('reaction')} className="input" placeholder="np. Wysypka, Obrzęk..." />
        </div>
        <div>
          <label className="label">Nasilenie</label>
          <select {...register('severity')} className="input">
            <option value="low">Niskie (low)</option>
            <option value="moderate">Umiarkowane (moderate)</option>
            <option value="high">Wysokie (high)</option>
          </select>
        </div>
        <div className="flex justify-end gap-3 pt-1">
          <button type="button" className="btn-secondary" onClick={onClose}>Anuluj</button>
          <button type="submit" className="btn-primary" disabled={mutation.isPending}>
            {mutation.isPending ? 'Zapisywanie...' : 'Dodaj'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

// ── Chronic disease form ──────────────────────────────────────────────────
const chronicSchema = z.object({
  diseaseName: z.string().min(1, 'Wymagane').max(200),
  icdCode: z.string().max(20).optional().or(z.literal('')),
  diagnosedDate: z.string().optional().or(z.literal('')),
  notes: z.string().max(1000).optional().or(z.literal('')),
  status: z.enum(['ACTIVE', 'REMISSION', 'RESOLVED', '']).optional(),
});

function AddChronicModal({ open, onClose, patientId }) {
  const qc = useQueryClient();
  const { register, handleSubmit, reset, formState: { errors } } = useForm({
    resolver: zodResolver(chronicSchema),
    defaultValues: { status: 'ACTIVE' },
  });

  const mutation = useMutation({
    mutationFn: (data) => chronicDiseaseApi.create({ ...data, patientId }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['chronic', patientId] });
      toast.success('Choroba przewlekła dodana');
      reset();
      onClose();
    },
    onError: (e) => toast.error(e.response?.data?.message || 'Błąd dodawania choroby'),
  });

  return (
    <Modal open={open} onClose={onClose} title="Dodaj chorobę przewlekłą" size="sm">
      <form onSubmit={handleSubmit((d) => mutation.mutate(d))} noValidate className="space-y-4">
        <div>
          <label className="label">Nazwa choroby *</label>
          <input {...register('diseaseName')} className={`input ${errors.diseaseName ? 'input-error' : ''}`}
            placeholder="np. Nadciśnienie tętnicze, Cukrzyca typu 2..." />
          {errors.diseaseName && <p className="error-msg">{errors.diseaseName.message}</p>}
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="label">Kod ICD-10</label>
            <input {...register('icdCode')} className="input font-mono" placeholder="np. I10, E11" />
          </div>
          <div>
            <label className="label">Data diagnozy</label>
            <input {...register('diagnosedDate')} type="date" className="input" />
          </div>
        </div>
        <div>
          <label className="label">Status</label>
          <select {...register('status')} className="input">
            <option value="ACTIVE">Aktywna</option>
            <option value="REMISSION">Remisja</option>
            <option value="RESOLVED">Wyleczona</option>
          </select>
        </div>
        <div>
          <label className="label">Notatki</label>
          <textarea {...register('notes')} rows={2} className="input" placeholder="Dodatkowe informacje..." />
        </div>
        <div className="flex justify-end gap-3 pt-1">
          <button type="button" className="btn-secondary" onClick={onClose}>Anuluj</button>
          <button type="submit" className="btn-primary" disabled={mutation.isPending}>
            {mutation.isPending ? 'Zapisywanie...' : 'Dodaj'}
          </button>
        </div>
      </form>
    </Modal>
  );
}

export default function PatientDetailPage() {
  const { id } = useParams();
  const { isAdmin, isDoctor } = useAuth();
  const [allergyModalOpen, setAllergyModalOpen] = useState(false);
  const [chronicModalOpen, setChronicModalOpen] = useState(false);
  const [labOrderModalOpen, setLabOrderModalOpen] = useState(false);
  const [labTab, setLabTab] = useState('orders'); // 'orders' | 'results'

  const { data: patient, isLoading } = useQuery({
    queryKey: ['patient', id],
    queryFn: () => patientApi.get(id).then(r => r.data),
  });

  const { data: allergies } = useQuery({
    queryKey: ['allergies', id],
    queryFn: () => allergyApi.list({ patientId: id }, { page: 0, size: 20 }).then(r => r.data),
    enabled: !!id,
  });

  const { data: visits } = useQuery({
    queryKey: ['visits', id],
    queryFn: () => visitApi.list({ patientId: id }, { page: 0, size: 5, sort: 'visitDate,desc' }).then(r => r.data),
    enabled: !!id,
  });

  const { data: prescriptions } = useQuery({
    queryKey: ['prescriptions', id],
    queryFn: () => prescriptionApi.list({ patientId: id }, { page: 0, size: 5, sort: 'issuedDate,desc' }).then(r => r.data),
    enabled: !!id,
  });

  const { data: labs } = useQuery({
    queryKey: ['labs', id],
    queryFn: () => labResultApi.list({ patientId: id }, { page: 0, size: 5, sort: 'resultDate,desc' }).then(r => r.data),
    enabled: !!id,
  });

  const { data: labOrders } = useQuery({
    queryKey: ['labOrders', 'patient', id],
    queryFn: () => labOrderApi.list({ patientId: id }, { page: 0, size: 5, sort: 'orderedAt,desc' }).then(r => r.data),
    enabled: !!id,
  });

  const { data: chronic } = useQuery({
    queryKey: ['chronic', id],
    queryFn: () => chronicDiseaseApi.list({ patientId: id }, { page: 0, size: 20 }).then(r => r.data),
    enabled: !!id,
  });

  if (isLoading) return <LoadingOverlay />;

  const genderLabel = { M: 'Mężczyzna', F: 'Kobieta', OTHER: 'Inne' };
  const contact = patient?.contactInfo || {};

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <Link to="/patients" className="btn-ghost p-2 text-gray-500">
          <ArrowLeft size={18} />
        </Link>
        <div>
          <h1 className="page-title">{patient?.firstName} {patient?.lastName}</h1>
          <p className="text-sm text-gray-500">Szczegóły pacjenta</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Patient info */}
        <div className="card">
          <div className="flex items-center gap-3 mb-4">
            <div className="w-12 h-12 rounded-xl bg-primary-100 flex items-center justify-center text-primary-700 text-xl font-bold">
              {patient?.firstName?.[0]?.toUpperCase() || '?'}
            </div>
            <div>
              <h2 className="section-title">{patient?.firstName} {patient?.lastName}</h2>
              <p className="text-xs text-gray-400">Pacjent</p>
            </div>
          </div>
          <div>
            {(isAdmin || isDoctor) && patient?.pesel && (
              <InfoRow label="PESEL" value={patient.pesel} />
            )}
            <InfoRow label="Data urodzenia" value={patient?.dateOfBirth} />
            <InfoRow label="Płeć" value={genderLabel[patient?.gender] || patient?.gender} />
            <InfoRow label="Adres" value={patient?.address} />
            {contact.phone && (
              <div className="flex justify-between py-2 border-b border-gray-50">
                <span className="text-sm text-gray-500 flex items-center gap-1"><Phone size={12} /> Telefon</span>
                <a href={`tel:${contact.phone}`} className="text-sm font-medium text-primary-600">{contact.phone}</a>
              </div>
            )}
            {contact.email && (
              <div className="flex justify-between py-2 border-b border-gray-50">
                <span className="text-sm text-gray-500 flex items-center gap-1"><Mail size={12} /> E-mail</span>
                <a href={`mailto:${contact.email}`} className="text-sm font-medium text-primary-600">{contact.email}</a>
              </div>
            )}
            {contact.emergencyContact && (
              <InfoRow label="Kontakt awaryjny" value={contact.emergencyContact} />
            )}
          </div>
        </div>

        {/* Allergies */}
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="section-title flex items-center gap-2">
              <AlertTriangle size={16} className="text-orange-500" />
              Alergie ({allergies?.totalElements || 0})
            </h2>
            {(isAdmin || isDoctor) && (
              <button className="btn-primary text-xs py-1" onClick={() => setAllergyModalOpen(true)}>
                <Plus size={12} /> Dodaj
              </button>
            )}
          </div>
          {allergies?.content?.length === 0 ? (
            <p className="text-sm text-gray-400">Brak zarejestrowanych alergii</p>
          ) : (
            <div className="space-y-2">
              {allergies?.content?.map((a) => (
                <div key={a.id} className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-800">{a.allergen}</p>
                    {a.reaction && <p className="text-xs text-gray-500">{a.reaction}</p>}
                  </div>
                  <span className={`badge ${severityColor[a.severity?.toLowerCase()] || severityColor[a.severity?.toUpperCase()] || 'badge-gray'}`}>
                    {a.severity || '—'}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Chronic diseases */}
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="section-title flex items-center gap-2">
              <Heart size={16} className="text-red-500" />
              Choroby przewlekłe ({chronic?.totalElements || 0})
            </h2>
            {(isAdmin || isDoctor) && (
              <button className="btn-primary text-xs py-1" onClick={() => setChronicModalOpen(true)}>
                <Plus size={12} /> Dodaj
              </button>
            )}
          </div>
          {chronic?.content?.length === 0 ? (
            <p className="text-sm text-gray-400">Brak chorób przewlekłych</p>
          ) : (
            <div className="space-y-2">
              {chronic?.content?.map((c) => (
                <div key={c.id} className="flex items-center justify-between">
                  <div>
                    <p className="text-sm font-medium text-gray-800">{c.diseaseName}</p>
                    {c.icdCode && <p className="text-xs text-gray-400 font-mono">{c.icdCode}</p>}
                  </div>
                  <span className={`badge ${c.status === 'ACTIVE' ? 'badge-red' : 'badge-gray'}`}>
                    {c.status || '—'}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Recent visits */}
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h2 className="section-title flex items-center gap-2">
            <Calendar size={16} className="text-primary-600" />
            Ostatnie wizyty
          </h2>
          <Link to={`/visits?patientId=${id}`} className="text-xs text-primary-600">
            Wszystkie →
          </Link>
        </div>
        {visits?.content?.length === 0 ? (
          <p className="text-sm text-gray-400">Brak wizyt</p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            {visits?.content?.map((v) => (
              <div key={v.id} className="text-sm border-l-2 border-primary-200 pl-3 py-1">
                <p className="font-medium text-gray-800">
                  {v.visitDate ? format(new Date(v.visitDate), 'dd.MM.yyyy', { locale: pl }) : '—'}
                </p>
                <p className="text-xs text-gray-500">{v.visitType} • {v.status}</p>
                {v.doctorName && <p className="text-xs text-gray-400">Dr {v.doctorName}</p>}
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Prescriptions */}
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <h2 className="section-title flex items-center gap-2">
              <FileText size={16} className="text-purple-600" />
              Ostatnie recepty
            </h2>
            <div className="flex items-center gap-2">
              <Link to={`/patients/${id}/prescriptions`} className="btn-primary text-xs py-1">
                <Plus size={12} /> Dodaj
              </Link>
              <Link to={`/patients/${id}/prescriptions`} className="text-xs text-primary-600">
                Wszystkie →
              </Link>
            </div>
          </div>
          {prescriptions?.content?.length === 0 ? (
            <p className="text-sm text-gray-400">Brak recept</p>
          ) : (
            <div className="space-y-2">
              {prescriptions?.content?.map((rx) => (
                <div key={rx.id} className="flex items-center justify-between text-sm">
                  <div>
                    <p className="font-medium text-gray-800">
                      Wystawiona: {rx.issuedDate || '—'}
                    </p>
                    <p className="text-xs text-gray-500">Ważna do: {rx.expirationDate || '—'}</p>
                    {rx.items?.length > 0 && (
                      <p className="text-xs text-gray-400 mt-0.5">
                        {rx.items.map(it => it.medicationName || 'Lek').join(', ')}
                      </p>
                    )}
                  </div>
                  <Link to={`/patients/${id}/prescriptions`} className="btn-ghost p-1 text-primary-600 text-xs">
                    PDF
                  </Link>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Lab section with tabs: Orders (new) | Manual results (legacy) */}
        <div className="card">
          <div className="flex items-center justify-between mb-3">
            <h2 className="section-title flex items-center gap-2">
              <FlaskConical size={16} className="text-orange-600" />
              Badania laboratoryjne
            </h2>
            <div className="flex items-center gap-2">
              {(isAdmin || isDoctor) && (
                <button
                  className="btn-primary text-xs py-1"
                  onClick={() => { setLabTab('orders'); setLabOrderModalOpen(true); }}
                >
                  <Plus size={12} /> Nowe zlecenie
                </button>
              )}
              {labTab === 'orders' ? (
                <Link to={`/lab-orders?patientId=${id}`} className="text-xs text-primary-600">
                  Wszystkie →
                </Link>
              ) : (
                <Link to={`/patients/${id}/lab-results`} className="text-xs text-primary-600">
                  Wszystkie →
                </Link>
              )}
            </div>
          </div>

          {/* Tabs */}
          <div className="flex gap-1 mb-4 bg-gray-100 rounded-lg p-1 w-fit">
            <button
              className={`px-3 py-1.5 rounded-md text-xs font-medium transition-colors ${labTab === 'orders' ? 'bg-white shadow-sm text-primary-700' : 'text-gray-500 hover:text-gray-700'}`}
              onClick={() => setLabTab('orders')}
            >
              <span className="flex items-center gap-1.5">
                <ClipboardList size={13} />
                Zlecenia ({labOrders?.totalElements ?? 0})
              </span>
            </button>
            <button
              className={`px-3 py-1.5 rounded-md text-xs font-medium transition-colors ${labTab === 'results' ? 'bg-white shadow-sm text-primary-700' : 'text-gray-500 hover:text-gray-700'}`}
              onClick={() => setLabTab('results')}
            >
              <span className="flex items-center gap-1.5">
                <FlaskConical size={13} />
                Wyniki manualne ({labs?.totalElements ?? 0})
              </span>
            </button>
          </div>

          {/* Tab: Zlecenia laboratoryjne */}
          {labTab === 'orders' && (
            labOrders?.content?.length === 0 ? (
              <p className="text-sm text-gray-400">Brak zleceń laboratoryjnych</p>
            ) : (
              <div className="space-y-2">
                {labOrders?.content?.map((o) => (
                  <Link
                    key={o.id}
                    to={`/lab-orders/${o.id}`}
                    className="flex items-center justify-between text-sm hover:bg-gray-50 rounded-lg p-2 -mx-2 transition-colors group"
                  >
                    <div>
                      <p className="font-medium text-gray-800 group-hover:text-primary-700">
                        {o.orderedAt
                          ? format(new Date(o.orderedAt), 'dd.MM.yyyy HH:mm', { locale: pl })
                          : '—'}
                        {o.priority && o.priority !== 'routine' && (
                          <span className={`ml-2 badge ${o.priority === 'stat' ? 'badge-red' : 'badge-yellow'}`}>
                            {o.priority === 'stat' ? 'CITO' : 'Pilne'}
                          </span>
                        )}
                      </p>
                      <p className="text-xs text-gray-500">
                        {o.completedItemCount ?? 0}/{o.itemCount ?? 0} testów wykonanych
                        {o.orderedByUsername && ` • Dr ${o.orderedByUsername}`}
                      </p>
                    </div>
                    <span className={`badge ${
                      o.status === 'completed' ? 'badge-green'
                      : o.status === 'in_progress' ? 'badge-blue'
                      : o.status === 'cancelled' ? 'badge-gray'
                      : 'badge-yellow'
                    }`}>
                      {o.status === 'completed' ? 'Zakończone'
                        : o.status === 'in_progress' ? 'W trakcie'
                        : o.status === 'cancelled' ? 'Anulowane'
                        : 'Oczekujące'}
                    </span>
                  </Link>
                ))}
              </div>
            )
          )}

          {/* Tab: Wyniki manualne (legacy lab_results) */}
          {labTab === 'results' && (
            labs?.content?.length === 0 ? (
              <div>
                <p className="text-sm text-gray-400 mb-2">Brak ręcznie dodanych wyników</p>
                <Link to={`/patients/${id}/lab-results`} className="btn-secondary text-xs py-1">
                  <Plus size={12} /> Dodaj wynik manualnie
                </Link>
              </div>
            ) : (
              <div className="space-y-2">
                {labs?.content?.map((l) => (
                  <div key={l.id} className="flex items-center justify-between text-sm">
                    <div>
                      <p className="font-medium text-gray-800">{l.testName}</p>
                      <p className="text-xs text-gray-500">{l.resultDate} • {l.unit || '—'}</p>
                    </div>
                    <span className={`badge ${l.status === 'completed' ? 'badge-green' : 'badge-yellow'}`}>
                      {l.result || l.status}
                    </span>
                  </div>
                ))}
              </div>
            )
          )}
        </div>
      </div>
      <ConsentPanel patientId={id} />

      <AddAllergyModal open={allergyModalOpen} onClose={() => setAllergyModalOpen(false)} patientId={id} />
      <AddChronicModal open={chronicModalOpen} onClose={() => setChronicModalOpen(false)} patientId={id} />
      <LabOrderCreateModal
        open={labOrderModalOpen}
        onClose={() => setLabOrderModalOpen(false)}
        initialPatientId={id}
        patientName={patient ? `${patient.firstName || ''} ${patient.lastName || ''}`.trim() : undefined}
      />
    </div>
  );
}
