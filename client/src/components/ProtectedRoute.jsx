import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { LoadingOverlay } from './Spinner';

export function ProtectedRoute({ children, roles }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  // Wait until the boot-time session check completes to avoid a redirect
  // flicker when a refresh token is being silently exchanged.
  if (loading) {
    return <LoadingOverlay />;
  }

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (roles && !roles.includes(user.role?.toLowerCase())) {
    return <Navigate to="/403" replace />;
  }

  return children;
}

