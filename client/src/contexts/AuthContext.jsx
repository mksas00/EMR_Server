import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authApi } from '../api';

const AuthContext = createContext(null);

function parseJwt(token) {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(base64));
  } catch {
    return null;
  }
}

// Returns true if the JWT has no `exp` or it is in the past (with small skew).
function isTokenExpired(token, skewSeconds = 5) {
  const payload = parseJwt(token);
  if (!payload || !payload.exp) return true;
  const nowSec = Math.floor(Date.now() / 1000);
  return payload.exp - skewSeconds <= nowSec;
}

function userFromToken(token) {
  const payload = parseJwt(token);
  if (!payload) return null;
  return {
    id: payload.sub,
    username: payload.uname || payload.sub,
    role: payload.role || 'USER',
    email: payload.email,
  };
}

function clearAuthStorage() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
}

export function AuthProvider({ children }) {
  // Initial state: only treat user as authenticated if access token is present
  // AND not expired. Otherwise we will try to refresh in useEffect below.
  const [user, setUser] = useState(() => {
    const token = localStorage.getItem('accessToken');
    if (!token || isTokenExpired(token)) return null;
    const u = userFromToken(token);
    if (u) localStorage.setItem('user', JSON.stringify(u));
    return u;
  });
  // `loading` is true while we are verifying / refreshing the session at boot.
  const [loading, setLoading] = useState(true);

  // Boot-time session verification. If access token is expired but a refresh
  // token exists, try to silently refresh; otherwise clear storage so that
  // ProtectedRoute can redirect to /login.
  useEffect(() => {
    let cancelled = false;
    (async () => {
      const access = localStorage.getItem('accessToken');
      const refresh = localStorage.getItem('refreshToken');

      // Case 1: nothing to do – no tokens at all
      if (!access && !refresh) {
        if (!cancelled) {
          clearAuthStorage();
          setUser(null);
          setLoading(false);
        }
        return;
      }

      // Case 2: valid access token – session OK
      if (access && !isTokenExpired(access)) {
        if (!cancelled) setLoading(false);
        return;
      }

      // Case 3: access token missing/expired but we have a refresh token
      if (refresh) {
        try {
          const { data } = await authApi.refresh({ refreshToken: refresh });
          if (cancelled) return;
          const newAccess = data.accessToken;
          const newRefresh = data.refreshToken || refresh;
          localStorage.setItem('accessToken', newAccess);
          localStorage.setItem('refreshToken', newRefresh);
          const u = userFromToken(newAccess);
          if (u) {
            localStorage.setItem('user', JSON.stringify(u));
            setUser(u);
          } else {
            clearAuthStorage();
            setUser(null);
          }
        } catch {
          if (!cancelled) {
            clearAuthStorage();
            setUser(null);
          }
        } finally {
          if (!cancelled) setLoading(false);
        }
        return;
      }

      // Case 4: only an expired access token, no refresh – drop session
      if (!cancelled) {
        clearAuthStorage();
        setUser(null);
        setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    const handler = () => {
      clearAuthStorage();
      setUser(null);
    };
    window.addEventListener('auth:logout', handler);
    return () => window.removeEventListener('auth:logout', handler);
  }, []);

  const login = useCallback(async (credentials) => {
    setLoading(true);
    try {
      const { data } = await authApi.login(credentials);
      if (data.mfaRequired) {
        return { mfaRequired: true, challengeToken: data.challengeToken };
      }
      const payload = parseJwt(data.accessToken);
      const userObj = {
        id: payload?.sub,
        username: payload?.uname || payload?.sub,
        role: payload?.role || 'USER',
        email: payload?.email,
      };
      localStorage.setItem('accessToken', data.accessToken);
      localStorage.setItem('refreshToken', data.refreshToken);
      localStorage.setItem('user', JSON.stringify(userObj));
      setUser(userObj);
      return { success: true };
    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(async () => {
    try {
      const rt = localStorage.getItem('refreshToken');
      if (rt) await authApi.logout(rt).catch(() => {});
    } finally {
      clearAuthStorage();
      setUser(null);
    }
  }, []);

  const isAdmin = user?.role?.toLowerCase() === 'admin';
  const isDoctor = user?.role?.toLowerCase() === 'doctor';

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, isAdmin, isDoctor }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
};

