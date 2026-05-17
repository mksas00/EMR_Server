import { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'react-toastify';
import { Eye, EyeOff, Stethoscope, Lock, User, ShieldCheck } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import { authApi } from '../../api';
import { Spinner } from '../../components/Spinner';

const loginSchema = z.object({
  username: z.string().min(1, 'Podaj nazwę użytkownika').max(50).regex(/^[a-zA-Z0-9._@-]+$/, 'Niedozwolone znaki'),
  password: z.string().min(1, 'Podaj hasło').max(100),
});

const mfaSchema = z.object({
  totp: z.string().length(6, 'Kod TOTP musi mieć 6 cyfr').regex(/^\d{6}$/, 'Kod musi składać się z cyfr'),
});

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from?.pathname || '/';

  const [showPassword, setShowPassword] = useState(false);
  const [mfaState, setMfaState] = useState(null); // { challengeToken, credentials }
  const [submitting, setSubmitting] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm({
    resolver: zodResolver(loginSchema),
  });
  const { register: regMfa, handleSubmit: handleMfaSubmit, formState: { errors: mfaErrors } } = useForm({
    resolver: zodResolver(mfaSchema),
  });

  const onSubmit = async (data) => {
    setSubmitting(true);
    // Backend expects "usernameOrEmail" field
    const payload = { usernameOrEmail: data.username, password: data.password };
    try {
      const result = await login(payload);
      if (result.mfaRequired) {
        setMfaState({ challengeToken: result.challengeToken, credentials: payload });
        return;
      }
      if (result.success) {
        toast.success('Zalogowano pomyślnie!');
        navigate(from, { replace: true });
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data?.error || 'Błędny login lub hasło';
      toast.error(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const onMfaSubmit = async (data) => {
    setSubmitting(true);
    try {
      const { data: authData } = await authApi.login({
        ...mfaState.credentials,
        mfaCode: data.totp,
      });
      const payload = JSON.parse(atob(authData.accessToken.split('.')[1]));
      const userObj = {
        id: payload?.sub,
        username: payload?.uname || payload?.sub,
        role: payload?.role || 'USER',
        email: payload?.email,
      };
      localStorage.setItem('accessToken', authData.accessToken);
      localStorage.setItem('refreshToken', authData.refreshToken);
      localStorage.setItem('user', JSON.stringify(userObj));
      window.location.href = from;
    } catch (err) {
      const msg = err.response?.data?.message || 'Nieprawidłowy kod MFA';
      toast.error(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-800 via-primary-700 to-primary-900 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-white/10 rounded-2xl mb-4">
            <Stethoscope size={32} className="text-white" />
          </div>
          <h1 className="text-3xl font-bold text-white">EMR System</h1>
          <p className="text-primary-200 mt-1 text-sm">Elektroniczna dokumentacja medyczna</p>
        </div>

        <div className="bg-white rounded-2xl shadow-2xl p-8">
          {!mfaState ? (
            <>
              <h2 className="text-xl font-semibold text-gray-800 mb-6">Logowanie</h2>
              <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
                <div>
                  <label className="label">Nazwa użytkownika</label>
                  <div className="relative">
                    <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">
                      <User size={16} />
                    </span>
                    <input
                      {...register('username')}
                      type="text"
                      className={`input pl-9 ${errors.username ? 'input-error' : ''}`}
                      placeholder="login"
                      autoComplete="username"
                      autoFocus
                    />
                  </div>
                  {errors.username && <p className="error-msg">{errors.username.message}</p>}
                </div>

                <div>
                  <label className="label">Hasło</label>
                  <div className="relative">
                    <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400">
                      <Lock size={16} />
                    </span>
                    <input
                      {...register('password')}
                      type={showPassword ? 'text' : 'password'}
                      className={`input pl-9 pr-10 ${errors.password ? 'input-error' : ''}`}
                      placeholder="••••••••"
                      autoComplete="current-password"
                    />
                    <button
                      type="button"
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                      onClick={() => setShowPassword(!showPassword)}
                    >
                      {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                  {errors.password && <p className="error-msg">{errors.password.message}</p>}
                </div>

                <div className="flex justify-end">
                  <Link to="/forgot-password" className="text-sm text-primary-600 hover:text-primary-700">
                    Zapomniałem hasła
                  </Link>
                </div>

                <button type="submit" className="btn-primary w-full justify-center py-2.5" disabled={submitting}>
                  {submitting ? <Spinner size="sm" /> : 'Zaloguj się'}
                </button>
              </form>
            </>
          ) : (
            <>
              <div className="flex items-center gap-3 mb-6">
                <div className="w-10 h-10 bg-primary-100 rounded-xl flex items-center justify-center">
                  <ShieldCheck size={20} className="text-primary-600" />
                </div>
                <div>
                  <h2 className="text-lg font-semibold text-gray-800">Weryfikacja dwuetapowa</h2>
                  <p className="text-sm text-gray-500">Wprowadź kod z aplikacji uwierzytelniającej</p>
                </div>
              </div>
              <form onSubmit={handleMfaSubmit(onMfaSubmit)} noValidate className="space-y-4">
                <div>
                  <label className="label">Kod TOTP (6 cyfr)</label>
                  <input
                    {...regMfa('totp')}
                    type="text"
                    inputMode="numeric"
                    maxLength={6}
                    className={`input text-center text-2xl tracking-widest font-mono ${mfaErrors.totp ? 'input-error' : ''}`}
                    placeholder="000000"
                    autoFocus
                    autoComplete="one-time-code"
                  />
                  {mfaErrors.totp && <p className="error-msg">{mfaErrors.totp.message}</p>}
                </div>
                <button type="submit" className="btn-primary w-full justify-center py-2.5" disabled={submitting}>
                  {submitting ? <Spinner size="sm" /> : 'Weryfikuj'}
                </button>
                <button
                  type="button"
                  className="btn-ghost w-full justify-center text-gray-500"
                  onClick={() => setMfaState(null)}
                >
                  ← Wróć do logowania
                </button>
              </form>
            </>
          )}
        </div>

        <p className="text-center text-primary-200 text-xs mt-6">
          Dostęp wyłącznie dla upoważnionego personelu medycznego
        </p>
      </div>
    </div>
  );
}
