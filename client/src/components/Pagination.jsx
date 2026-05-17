import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-react';

export function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) return null;

  return (
    <div className="flex items-center justify-between px-4 py-3 border-t border-gray-100">
      <p className="text-sm text-gray-500">
        Strona <span className="font-medium">{page + 1}</span> z <span className="font-medium">{totalPages}</span>
      </p>
      <div className="flex items-center gap-1">
        <button className="btn-ghost px-2 py-1 text-gray-500 disabled:opacity-30" onClick={() => onPageChange(0)} disabled={page === 0}>
          <ChevronsLeft size={16} />
        </button>
        <button className="btn-ghost px-2 py-1 text-gray-500 disabled:opacity-30" onClick={() => onPageChange(page - 1)} disabled={page === 0}>
          <ChevronLeft size={16} />
        </button>
        {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
          let pageNum;
          if (totalPages <= 5) pageNum = i;
          else if (page < 3) pageNum = i;
          else if (page > totalPages - 4) pageNum = totalPages - 5 + i;
          else pageNum = page - 2 + i;
          return (
            <button
              key={pageNum}
              className={`w-8 h-8 text-sm rounded-lg font-medium transition-colors ${pageNum === page ? 'bg-primary-600 text-white' : 'text-gray-600 hover:bg-gray-100'}`}
              onClick={() => onPageChange(pageNum)}
            >
              {pageNum + 1}
            </button>
          );
        })}
        <button className="btn-ghost px-2 py-1 text-gray-500 disabled:opacity-30" onClick={() => onPageChange(page + 1)} disabled={page >= totalPages - 1}>
          <ChevronRight size={16} />
        </button>
        <button className="btn-ghost px-2 py-1 text-gray-500 disabled:opacity-30" onClick={() => onPageChange(totalPages - 1)} disabled={page >= totalPages - 1}>
          <ChevronsRight size={16} />
        </button>
      </div>
    </div>
  );
}
