import { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Users, Calendar, FileText, FlaskConical, FolderOpen, TrendingUp, Clock, Pill, Download, CheckCircle2 } from 'lucide-react';
import { patientApi, visitApi, prescriptionApi, labResultApi } from '../api';
import axiosClient from '../api/axiosClient';
import { useAuth } from '../contexts/AuthContext';
import { LoadingOverlay } from '../components/Spinner';
import { format } from 'date-fns';
import { pl } from 'date-fns/locale';

function StatCard({ icon: Icon, label, value, color, to }) {
  return (
    <Link to={to} className={`card flex items-center gap-4 hover:shadow-md transition-shadow group`}>
      <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${color}`}>
        <Icon size={22} className="text-white" />
      </div>
      <div>
        <p className="text-sm text-gray-500">{label}</p>
        <p className="text-2xl font-bold text-gray-800 group-hover:text-primary-600 transition-colors">
          {value ?? '—'}
        </p>
      </div>
    </Link>
  );
}

export default function DashboardPage() {
  const { user, isDoctor, isAdmin } = useAuth();
  const [importResult, setImportResult] = useState(null);

  const { data: patients, isLoading: loadingP } = useQuery({
    queryKey: ['patients', 'count'],
    queryFn: () => patientApi.list({}, { page: 0, size: 1 }).then(r => r.data),
  });

  const { data: visits, isLoading: loadingV } = useQuery({
    queryKey: ['visits', 'upcoming'],
    queryFn: () => visitApi.list(
      { status: 'PLANNED', doctorId: isDoctor ? user?.id : undefined },
      { page: 0, size: 5, sort: 'visitDate,asc' }
    ).then(r => r.data),
  });

  const { data: prescriptions, isLoading: loadingRx } = useQuery({
    queryKey: ['prescriptions', 'count'],
    queryFn: () => prescriptionApi.list({}, { page: 0, size: 1 }).then(r => r.data),
  });

  const { data: labResults, isLoading: loadingLab } = useQuery({
    queryKey: ['labResults', 'pending'],
    queryFn: () => labResultApi.list({ status: 'pending' }, { page: 0, size: 1 }).then(r => r.data),
  });

  const importMedsMutation = useMutation({
    mutationFn: ({ startPage, size, maxPages }) =>
      axiosClient.post(`/api/leki/import-urpl/bulk?startPage=${startPage}&size=${size}&maxPages=${maxPages}`),
    onSuccess: (r) => setImportResult(r.data),
    onError: () => setImportResult('Błąd importu'),
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="page-title">Dashboard</h1>
        <p className="text-gray-500 text-sm mt-1">
          Witaj, <span className="font-medium text-gray-700">{user?.username}</span>!
          Dzisiaj jest {format(new Date(), "EEEE, d MMMM yyyy", { locale: pl })}.
        </p>
      </div>

      {/* Stats grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        <StatCard icon={Users} label="Pacjenci" value={patients?.totalElements} color="bg-blue-500" to="/patients" />
        <StatCard icon={Calendar} label="Nadchodzące wizyty" value={visits?.totalElements} color="bg-green-500" to="/visits" />
        <StatCard icon={FileText} label="Recepty" value={prescriptions?.totalElements} color="bg-purple-500" to="/prescriptions" />
        <StatCard icon={FlaskConical} label="Wyniki oczekujące" value={labResults?.totalElements} color="bg-orange-500" to="/lab-results" />
      </div>

      {/* Upcoming visits */}
      <div className="card">
        <div className="flex items-center justify-between mb-4">
          <h2 className="section-title flex items-center gap-2">
            <Clock size={18} className="text-primary-600" />
            Nadchodzące wizyty
          </h2>
          <Link to="/visits" className="text-sm text-primary-600 hover:text-primary-700 font-medium">
            Zobacz wszystkie →
          </Link>
        </div>

        {loadingV ? (
          <LoadingOverlay />
        ) : visits?.content?.length === 0 ? (
          <div className="text-center py-8 text-gray-400">
            <Calendar size={32} className="mx-auto mb-2 opacity-40" />
            <p className="text-sm">Brak nadchodzących wizyt</p>
          </div>
        ) : (
          <div className="space-y-2">
            {visits?.content?.map((visit) => (
              <Link
                key={visit.id}
                to={`/visits`}
                className="flex items-center gap-4 p-3 rounded-lg hover:bg-gray-50 transition-colors group"
              >
                <div className="w-10 h-10 rounded-lg bg-primary-100 flex items-center justify-center shrink-0">
                  <Calendar size={16} className="text-primary-600" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-gray-800 truncate">
                    {visit.patientFullName || visit.patientId || '—'}
                  </p>
                  <p className="text-xs text-gray-500">
                    {visit.visitDate
                      ? format(new Date(visit.visitDate), 'dd.MM.yyyy HH:mm', { locale: pl })
                      : '—'}
                    {visit.visitType && ` • ${visit.visitType}`}
                  </p>
                </div>
                <span className={`badge ${visit.status === 'CONFIRMED' ? 'badge-green' : 'badge-blue'}`}>
                  {visit.status}
                </span>
              </Link>
            ))}
          </div>
        )}
      </div>

      {/* Quick actions */}
      <div className="card">
        <h2 className="section-title mb-4">Szybkie akcje</h2>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {[
            { label: 'Nowy pacjent', icon: Users, to: '/patients', color: 'text-blue-600 bg-blue-50 hover:bg-blue-100' },
            { label: 'Nowa wizyta', icon: Calendar, to: '/visits', color: 'text-green-600 bg-green-50 hover:bg-green-100' },
            { label: 'Nowa recepta', icon: FileText, to: '/prescriptions', color: 'text-purple-600 bg-purple-50 hover:bg-purple-100' },
            { label: 'Wyniki lab.', icon: FlaskConical, to: '/lab-results', color: 'text-orange-600 bg-orange-50 hover:bg-orange-100' },
          ].map((a) => (
            <Link
              key={a.label}
              to={a.to}
              className={`flex flex-col items-center gap-2 p-4 rounded-xl font-medium text-sm transition-colors ${a.color}`}
            >
              <a.icon size={22} />
              {a.label}
            </Link>
          ))}
        </div>
      </div>
      {/* Admin: import leków URPL */}
      {isAdmin && (
        <div className="card border border-amber-200 bg-amber-50">
          <h2 className="section-title flex items-center gap-2 mb-4 text-amber-800">
            <Pill size={18} className="text-amber-600" />
            Administracja – baza leków URPL
          </h2>
          <p className="text-sm text-amber-700 mb-4">
            Importuj dane leków z rejestru URPL (ezdrowie.gov.pl). Import pobiera kolejne strony z publicznego API.
          </p>
          <div className="flex flex-wrap gap-3 items-center">
            <button
              className="btn-primary bg-amber-600 hover:bg-amber-700 border-amber-600"
              onClick={() => importMedsMutation.mutate({ startPage: 0, size: 100, maxPages: 5 })}
              disabled={importMedsMutation.isPending}
            >
              <Download size={15} />
              {importMedsMutation.isPending ? 'Importowanie...' : 'Import 500 rekordów (5 stron)'}
            </button>
            <button
              className="btn-secondary"
              onClick={() => importMedsMutation.mutate({ startPage: 0, size: 100, maxPages: 50 })}
              disabled={importMedsMutation.isPending}
            >
              Import 5000 rekordów (50 stron)
            </button>
          </div>
          {importResult && (
            <div className="mt-3 flex items-center gap-2 text-sm text-green-700">
              <CheckCircle2 size={16} /> {importResult}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
