import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Shield, CheckCircle, XCircle, Clock, User } from 'lucide-react';
import { format, formatDistanceToNow } from 'date-fns';
import { pl } from 'date-fns/locale';
import { consentApi } from '../../api';
import { LoadingOverlay } from '../../components/Spinner';
import { Pagination } from '../../components/Pagination';

const SCOPE_LABEL = { read: 'Odczyt', write: 'Odczyt + Zapis' };
const SCOPE_BADGE = { read: 'badge-blue', write: 'badge-yellow' };

/**
 * Strona dla lekarza/pielęgniarki/lab_tech: lista pacjentów, do których mam nadany dostęp
 * przez opiekuna (RODO – transparentność dostępu).
 */
export default function ConsentsMePage() {
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ['consents', 'mine', page],
    queryFn: () =>
      consentApi.listMine({ page, size: 15, sort: 'grantedAt,desc' }).then(r => r.data),
    keepPreviousData: true,
  });

  const consents = data?.content || [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="page-title flex items-center gap-2">
          <Shield size={22} /> Moje dostępy do pacjentów
        </h1>
        <p className="text-sm text-gray-500 mt-1">
          Aktywne zgody dostępu do danych pacjentów, które przyznali Ci opiekunowie (RODO)
        </p>
      </div>

      <div className="card p-0 overflow-hidden">
        {isLoading && <LoadingOverlay />}
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr>
                <th className="table-header">Pacjent</th>
                <th className="table-header">Zakres</th>
                <th className="table-header">Nadano</th>
                <th className="table-header">Wygasa</th>
                <th className="table-header">Status</th>
                <th className="table-header">Uzasadnienie</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {!isLoading && consents.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center py-12 text-gray-400">
                    <Shield size={36} className="mx-auto mb-2 opacity-30" />
                    <p className="text-sm">Brak aktywnych zgód dostępu</p>
                    <p className="text-xs mt-1">Gdy lekarz prowadzący nada Ci dostęp do pacjenta, pojawi się tutaj</p>
                  </td>
                </tr>
              ) : (
                consents.map(c => (
                  <tr key={c.id} className="hover:bg-gray-50">
                    <td className="table-cell">
                      <Link
                        to={`/patients/${c.patientId}`}
                        className="flex items-center gap-2 font-medium text-primary-600 hover:underline"
                      >
                        <User size={14} className="text-gray-400" />
                        {c.patientFullName || c.patientId?.slice(0, 8)}
                      </Link>
                    </td>
                    <td className="table-cell">
                      <span className={`badge ${SCOPE_BADGE[c.scope] || 'badge-gray'}`}>
                        {SCOPE_LABEL[c.scope] || c.scope}
                      </span>
                    </td>
                    <td className="table-cell text-sm text-gray-600">
                      {c.grantedAt ? format(new Date(c.grantedAt), 'dd.MM.yyyy HH:mm') : '—'}
                    </td>
                    <td className="table-cell text-sm">
                      {c.expiresAt ? (
                        <div>
                          <p>{format(new Date(c.expiresAt), 'dd.MM.yyyy HH:mm')}</p>
                          {c.active && (
                            <p className="text-xs text-amber-600 flex items-center gap-1">
                              <Clock size={10} />
                              {formatDistanceToNow(new Date(c.expiresAt), { locale: pl, addSuffix: true })}
                            </p>
                          )}
                        </div>
                      ) : (
                        <span className="text-xs text-gray-400">Bezterminowo</span>
                      )}
                    </td>
                    <td className="table-cell">
                      {c.active ? (
                        <span className="flex items-center gap-1 text-green-600 text-xs font-medium">
                          <CheckCircle size={13} /> Aktywna
                        </span>
                      ) : (
                        <span className="flex items-center gap-1 text-gray-400 text-xs">
                          <XCircle size={13} /> Wygasła
                        </span>
                      )}
                    </td>
                    <td className="table-cell text-xs text-gray-500 max-w-xs truncate" title={c.reason || ''}>
                      {c.reason || '—'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        <Pagination page={page} totalPages={data?.totalPages || 0} onPageChange={setPage} />
      </div>
    </div>
  );
}

