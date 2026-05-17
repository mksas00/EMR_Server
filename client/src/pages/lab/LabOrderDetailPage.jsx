import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-toastify';
import { ArrowLeft, FlaskConical, Save, XCircle, AlertTriangle, ChevronDown } from 'lucide-react';
import { format } from 'date-fns';
import { labOrderApi } from '../../api';
import { LoadingOverlay } from '../../components/Spinner';
import { ConfirmModal } from '../../components/Modal';
import { useAuth } from '../../contexts/AuthContext';

const STATUS_BADGE = {
  pending: 'badge-yellow', in_progress: 'badge-blue', completed: 'badge-green', cancelled: 'badge-gray',
};
const STATUS_LABEL = {
  pending: 'Oczekujące', in_progress: 'W trakcie', completed: 'Zakończone', cancelled: 'Anulowane',
};
const FLAG_BADGE = {
  N: 'badge-green', L: 'badge-yellow', H: 'badge-yellow', LL: 'badge-red', HH: 'badge-red',
};
const FLAG_LABEL = { N: 'Norma', L: 'Niski', H: 'Wysoki', LL: '↓↓ Krytycznie', HH: '↑↑ Krytycznie' };

function ItemRow({ item, canEdit, onSave }) {
  const [result, setResult] = useState(item.result || '');
  const [resultNumeric, setResultNumeric] = useState(item.resultNumeric ?? '');
  const [notes, setNotes] = useState(item.notes || '');
  const [saving, setSaving] = useState(false);
  const [expanded, setExpanded] = useState(false);

  const isCompleted = item.status === 'completed';
  const isCancelled = item.status === 'cancelled';

  const refRangeText = item.refRangeText
    || (item.refRangeLow != null && item.refRangeHigh != null
        ? `${item.refRangeLow} – ${item.refRangeHigh}`
        : item.refRangeLow != null ? `≥ ${item.refRangeLow}`
        : item.refRangeHigh != null ? `≤ ${item.refRangeHigh}` : '—');

  const handleSave = async () => {
    setSaving(true);
    try {
      const numeric = resultNumeric === '' ? null : parseFloat(resultNumeric);
      await onSave(item.id, {
        result: result || null,
        resultNumeric: Number.isFinite(numeric) ? numeric : null,
        notes: notes || null,
      });
    } finally {
      setSaving(false);
    }
  };

  return (
    <tr className={`${isCancelled ? 'opacity-50' : ''} hover:bg-gray-50`}>
      <td className="table-cell">
        <div>
          <p className="font-medium text-sm">{item.testName}</p>
          <p className="text-xs text-gray-400 font-mono">{item.testCode}</p>
        </div>
      </td>
      <td className="table-cell">
        {canEdit && !isCompleted && !isCancelled ? (
          <div className="flex flex-col gap-1">
            <input value={result} onChange={e => setResult(e.target.value)}
              placeholder="Wynik (tekst lub liczba)"
              className="input text-sm py-1 w-32" />
            <input value={resultNumeric} onChange={e => setResultNumeric(e.target.value)}
              placeholder="Wartość liczbowa (opc.)" type="number" step="any"
              className="input text-xs py-1 w-32" />
          </div>
        ) : (
          <p className="text-sm">{item.result || <span className="text-gray-400">—</span>}</p>
        )}
      </td>
      <td className="table-cell text-xs text-gray-500">{item.unit || '—'}</td>
      <td className="table-cell text-xs text-gray-500">{refRangeText}</td>
      <td className="table-cell">
        {item.abnormalFlag ? (
          <span className={`badge ${FLAG_BADGE[item.abnormalFlag] || 'badge-gray'} text-xs`}>
            {FLAG_LABEL[item.abnormalFlag] || item.abnormalFlag}
          </span>
        ) : <span className="text-xs text-gray-400">—</span>}
      </td>
      <td className="table-cell">
        <span className={`badge ${STATUS_BADGE[item.status] || 'badge-gray'} text-xs`}>
          {STATUS_LABEL[item.status] || item.status}
        </span>
      </td>
      <td className="table-cell text-xs text-gray-500">
        {item.completedAt ? (
          <div>
            <p>{format(new Date(item.completedAt), 'yyyy-MM-dd HH:mm')}</p>
            <p className="text-gray-400">{item.completedByUsername || ''}</p>
          </div>
        ) : '—'}
      </td>
      <td className="table-cell text-right">
        {canEdit && !isCompleted && !isCancelled && (
          <button onClick={handleSave} disabled={saving || !result}
            className="btn-primary text-xs py-1 px-2">
            {saving ? '...' : <><Save size={13} /> Zapisz</>}
          </button>
        )}
        {(item.notes || expanded) && (
          <button onClick={() => setExpanded(v => !v)} className="btn-ghost text-xs ml-1">
            <ChevronDown size={13} className={expanded ? 'rotate-180 transition' : 'transition'} />
          </button>
        )}
        {expanded && canEdit && !isCompleted && !isCancelled && (
          <textarea value={notes} onChange={e => setNotes(e.target.value)}
            placeholder="Notatka" className="input text-xs mt-2 w-48" rows={2} />
        )}
        {expanded && (isCompleted || isCancelled) && item.notes && (
          <p className="text-xs text-gray-500 mt-2 max-w-48">{item.notes}</p>
        )}
      </td>
    </tr>
  );
}

export default function LabOrderDetailPage() {
  const { id } = useParams();
  const qc = useQueryClient();
  const { user } = useAuth();
  const [cancelOpen, setCancelOpen] = useState(false);
  const [cancelReason, setCancelReason] = useState('');

  const { data: order, isLoading } = useQuery({
    queryKey: ['labOrders', 'detail', id],
    queryFn: () => labOrderApi.get(id).then(r => r.data),
  });

  const updateItem = useMutation({
    mutationFn: ({ itemId, data }) => labOrderApi.updateItem(itemId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['labOrders', 'detail', id] });
      qc.invalidateQueries({ queryKey: ['labOrders', 'list'] });
      toast.success('Wynik zapisany');
    },
    onError: (e) => toast.error(e.response?.data?.message || 'Błąd zapisu wyniku'),
  });

  const cancelMutation = useMutation({
    mutationFn: (reason) => labOrderApi.cancel(id, reason),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['labOrders'] });
      toast.success('Zlecenie anulowane');
      setCancelOpen(false);
    },
    onError: (e) => toast.error(e.response?.data?.message || 'Błąd anulowania'),
  });

  if (isLoading) return <LoadingOverlay />;
  if (!order) return (
    <div className="card p-12 text-center text-gray-400">
      <FlaskConical size={36} className="mx-auto mb-2 opacity-30" />
      <p>Zlecenie nie istnieje lub brak dostępu</p>
      <Link to="/lab-orders" className="btn-secondary mt-4 inline-flex"><ArrowLeft size={14} /> Powrót</Link>
    </div>
  );

  const role = user?.role;
  const canEditItems = ['lab_tech', 'doctor', 'admin'].includes(role);
  const canCancel = ['doctor', 'admin'].includes(role)
    && order.status !== 'completed' && order.status !== 'cancelled';

  const handleSaveItem = (itemId, data) => updateItem.mutateAsync({ itemId, data });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Link to="/lab-orders" className="btn-ghost text-gray-500"><ArrowLeft size={18} /></Link>
          <div>
            <h1 className="page-title flex items-center gap-2">
              <FlaskConical size={22} /> Zlecenie laboratoryjne
            </h1>
            <p className="text-xs text-gray-500 font-mono">{order.id}</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <span className={`badge ${STATUS_BADGE[order.status] || 'badge-gray'}`}>
            {STATUS_LABEL[order.status] || order.status}
          </span>
          {canCancel && (
            <button onClick={() => setCancelOpen(true)} className="btn-secondary text-red-600">
              <XCircle size={14} /> Anuluj zlecenie
            </button>
          )}
        </div>
      </div>

      {/* Header info */}
      <div className="card p-4 grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
        <div>
          <p className="text-xs text-gray-400">Pacjent</p>
          <Link to={`/patients/${order.patientId}`} className="font-medium text-primary-600 hover:underline">
            {order.patientFullName || order.patientId?.slice(0, 8)}
          </Link>
        </div>
        <div>
          <p className="text-xs text-gray-400">Zlecił</p>
          <p className="font-medium">{order.orderedByUsername || '—'}</p>
        </div>
        <div>
          <p className="text-xs text-gray-400">Zlecone</p>
          <p className="font-medium">{order.orderedAt ? format(new Date(order.orderedAt), 'yyyy-MM-dd HH:mm') : '—'}</p>
        </div>
        <div>
          <p className="text-xs text-gray-400">Postęp</p>
          <p className="font-medium">
            {order.completedItemCount} / {order.itemCount} ukończonych
            {order.abnormalItemCount > 0 && (
              <span className="ml-2 inline-flex items-center gap-1 text-red-600">
                <AlertTriangle size={12} /> {order.abnormalItemCount} nieprawidłowych
              </span>
            )}
          </p>
        </div>
        {order.notes && (
          <div className="col-span-2 md:col-span-4">
            <p className="text-xs text-gray-400">Notatka kliniczna</p>
            <p className="text-sm">{order.notes}</p>
          </div>
        )}
        {order.cancelledAt && (
          <div className="col-span-2 md:col-span-4 bg-red-50 rounded-lg p-3">
            <p className="text-xs text-red-700 font-medium">Anulowane</p>
            <p className="text-sm">
              {format(new Date(order.cancelledAt), 'yyyy-MM-dd HH:mm')} przez {order.cancelledByUsername}
              {order.cancelledReason && <> – {order.cancelledReason}</>}
            </p>
          </div>
        )}
      </div>

      {/* Items table */}
      <div className="card p-0 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr>
                <th className="table-header">Test</th>
                <th className="table-header">Wynik</th>
                <th className="table-header">Jednostka</th>
                <th className="table-header">Norma</th>
                <th className="table-header">Flaga</th>
                <th className="table-header">Status</th>
                <th className="table-header">Wykonano</th>
                <th className="table-header text-right">Akcje</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {(order.items || []).map(it => (
                <ItemRow key={it.id} item={it} canEdit={canEditItems} onSave={handleSaveItem} />
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <ConfirmModal
        open={cancelOpen}
        onClose={() => setCancelOpen(false)}
        onConfirm={() => cancelMutation.mutate(cancelReason)}
        title="Anuluj zlecenie"
        message={
          <div>
            <p className="mb-3">Czy na pewno chcesz anulować zlecenie? Wszystkie nieukończone pozycje zostaną oznaczone jako anulowane.</p>
            <input value={cancelReason} onChange={e => setCancelReason(e.target.value)}
              placeholder="Powód anulowania (opcjonalny)" className="input w-full" />
          </div>
        }
        danger
      />
    </div>
  );
}

