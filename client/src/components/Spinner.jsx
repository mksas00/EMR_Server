export function Spinner({ size = 'md', className = '' }) {
  const sizes = { sm: 'w-4 h-4', md: 'w-6 h-6', lg: 'w-10 h-10' };
  return (
    <div
      className={`border-2 border-primary-200 border-t-primary-600 rounded-full animate-spin ${sizes[size]} ${className}`}
    />
  );
}

export function LoadingOverlay() {
  return (
    <div className="flex items-center justify-center h-48">
      <Spinner size="lg" />
    </div>
  );
}

