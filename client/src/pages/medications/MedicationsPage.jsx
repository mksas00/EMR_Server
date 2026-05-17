import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Search, Pill, ChevronLeft, ChevronRight } from 'lucide-react';
import { medicationApi } from '../../api';
import { LoadingOverlay } from '../../components/Spinner';

const PAGE_SIZE = 20;

export default function MedicationsPage() {
  const [query, setQuery] = useState('');
  const [atc, setAtc] = useState('');
  const [inputQuery, setInputQuery] = useState('');
  const [inputAtc, setInputAtc] = useState('');
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState(null);

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ['medications', query, atc, page],
    queryFn: () =>
      medicationApi
        .search({ q: query || undefined, atc: atc || undefined, page, size: PAGE_SIZE, sort: 'name,asc' })
        .then(r => r.data),
    keepPreviousData: true,
  });

  const handleSearch = e => {
    e.preventDefault();
    setQuery(inputQuery.trim());
    setAtc(inputAtc.trim());
    setPage(0);
  };

  const totalPages = data?.totalPages || 0;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <Pill size={22} className="text-primary-600" />
        <h1 className="page-title">Baza leków (URPL)</h1>
      </div>

      {/* Search bar */}
      <div className="card p-4">
        <form onSubmit={handleSearch} className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
            <input
              value={inputQuery}
              onChange={e => setInputQuery(e.target.value)}
              className="input pl-9"
              placeholder="Szukaj po nazwie leku..."
            />
          </div>
          <div className="relative sm:w-48">
            <input
              value={inputAtc}
              onChange={e => setInputAtc(e.target.value)}
              className="input"
              placeholder="Kod ATC (np. C09)"
              maxLength={10}
            />
          </div>
          <button type="submit" className="btn-primary whitespace-nowrap">
            <Search size={15} /> Szukaj
          </button>
          {(query || atc) && (
            <button
              type="button"
              className="btn-ghost text-gray-500"
              onClick={() => {
                setQuery(''); setAtc('');
                setInputQuery(''); setInputAtc('');
                setPage(0);
              }}
            >
              Wyczyść
            </button>
          )}
        </form>
      </div>

      {/* Results */}
      <div className="card p-0 overflow-hidden">
        {(isLoading || isFetching) && <LoadingOverlay />}

        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr>
                <th className="table-header">Nazwa</th>
                <th className="table-header">Nazwa powszechna</th>
                <th className="table-header">Moc</th>
                <th className="table-header">Postać</th>
                <th className="table-header">ATC</th>
                <th className="table-header">Podmiot odpowiedzialny</th>
                <th className="table-header">Kategoria</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {!isLoading && data?.content?.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center py-16 text-gray-400">
                    <Pill size={36} className="mx-auto mb-2 opacity-30" />
                    <p className="text-sm">
                      {query || atc
                        ? `Brak wyników dla „${query || atc}"`
                        : 'Wpisz frazę i naciśnij Szukaj'}
                    </p>
                  </td>
                </tr>
              ) : (
                data?.content?.map(med => (
                  <tr
                    key={med.id}
                    className="hover:bg-primary-50 transition-colors cursor-pointer"
                    onClick={() => setSelected(selected?.id === med.id ? null : med)}
                  >
                    <td className="table-cell font-medium text-gray-900">
                      <span className="line-clamp-2">{med.name}</span>
                    </td>
                    <td className="table-cell text-gray-600 text-sm">{med.commonName || '—'}</td>
                    <td className="table-cell text-sm">{med.strength || '—'}</td>
                    <td className="table-cell text-sm">{med.pharmaceuticalForm || '—'}</td>
                    <td className="table-cell">
                      {med.atcCode ? (
                        <span className="badge badge-blue font-mono text-xs">{med.atcCode}</span>
                      ) : '—'}
                    </td>
                    <td className="table-cell text-xs text-gray-500 max-w-xs">
                      <span className="line-clamp-2">{med.marketingAuthorizationHolder || '—'}</span>
                    </td>
                    <td className="table-cell text-xs">
                      {med.prescriptionCategory ? (
                        <span className={`badge ${med.prescriptionCategory === 'Rp' || med.prescriptionCategory?.startsWith('Rp') ? 'badge-yellow' : 'badge-gray'}`}>
                          {med.prescriptionCategory}
                        </span>
                      ) : '—'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Expanded detail row */}
        {selected && (
          <div className="border-t border-primary-100 bg-primary-50 p-5">
            <h3 className="font-semibold text-gray-800 mb-3">{selected.name}</h3>
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-x-6 gap-y-2 text-sm">
              <Detail label="Nazwa powszechna" value={selected.commonName} />
              <Detail label="Moc" value={selected.strength} />
              <Detail label="Postać farmaceutyczna" value={selected.pharmaceuticalForm} />
              <Detail label="Kod ATC" value={selected.atcCode} />
              <Detail label="Substancje czynne" value={selected.activeSubstances} />
              <Detail label="Nr pozwolenia" value={selected.authorizationNumber} />
              <Detail label="Ważne do" value={selected.authorizationValidTo} />
              <Detail label="Podmiot odpowiedzialny" value={selected.marketingAuthorizationHolder} />
              <Detail label="Typ procedury" value={selected.procedureType} />
              <Detail label="Podstawa prawna" value={selected.legalBasis} />
              <Detail label="Gatunki docelowe" value={selected.targetSpecies} />
              <Detail label="Kategoria dostępności" value={selected.prescriptionCategory} />
            </div>
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-white">
            <span className="text-sm text-gray-500">
              Strona {page + 1} z {totalPages}
              {data?.totalElements !== undefined && (
                <span className="ml-2 text-gray-400">({data.totalElements.toLocaleString()} wyników)</span>
              )}
            </span>
            <div className="flex items-center gap-1">
              <button
                className="btn-ghost p-1.5 text-gray-500 disabled:opacity-40"
                disabled={page === 0}
                onClick={() => setPage(p => p - 1)}
              >
                <ChevronLeft size={16} />
              </button>
              {[...Array(Math.min(5, totalPages))].map((_, i) => {
                const p = Math.max(0, Math.min(page - 2, totalPages - 5)) + i;
                return (
                  <button
                    key={p}
                    onClick={() => setPage(p)}
                    className={`w-8 h-8 rounded text-sm font-medium transition-colors ${p === page ? 'bg-primary-600 text-white' : 'text-gray-600 hover:bg-gray-100'}`}
                  >
                    {p + 1}
                  </button>
                );
              })}
              <button
                className="btn-ghost p-1.5 text-gray-500 disabled:opacity-40"
                disabled={page >= totalPages - 1}
                onClick={() => setPage(p => p + 1)}
              >
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function Detail({ label, value }) {
  if (!value) return null;
  return (
    <div>
      <span className="text-gray-500 text-xs">{label}</span>
      <p className="text-gray-800 font-medium">{value}</p>
    </div>
  );
}

