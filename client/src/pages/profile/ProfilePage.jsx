import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'react-toastify';
import { Shield, Key, Smartphone, Monitor, Trash2, RefreshCw, Copy, LogOut } from 'lucide-react';
import { authApi } from '../../api';
import { useAuth } from '../../contexts/AuthContext';
import { LoadingOverlay } from '../../components/Spinner';
import { ConfirmModal, Modal } from '../../components/Modal';
import { QRCodeSVG } from 'qrcode.react';
import { format } from 'date-fns';
import { pl } from 'date-fns/locale';

const passwordSchema = z.object({
  currentPassword: z.string().min(1, 'Wymagane'),
  newPassword: z.string()
    .min(8, 'Min. 8 znaków')
    .max(100)
    .regex(/[A-Z]/, 'Musi zawierać wielką literę')
    .regex(/[0-9]/, 'Musi zawierać cyfrę')
    .regex(/[^a-zA-Z0-9]/, 'Musi zawierać znak specjalny'),
  confirmPassword: z.string(),
}).refine(d => d.newPassword === d.confirmPassword, {
  message: 'Hasła nie są identyczne',
  path: ['confirmPassword'],
});

const mfaConfirmSchema = z.object({
  code: z.string().length(6, '6 cyfr').regex(/^\d{6}$/),
});

// ── Section components ─────────────────────────────────────────────────────
function SectionHeader({ icon: Icon, title, color = 'text-primary-600' }) {
  return (
    <div className="flex items-center gap-2 mb-4">
      <Icon size={18} className={color} />
      <h2 className="section-title">{title}</h2>
    </div>
  );
}

// ── Change password ────────────────────────────────────────────────────────
function ChangePasswordSection() {
  const { register, handleSubmit, reset, formState: { errors } } = useForm({
    resolver: zodResolver(passwordSchema),
  });
  const mutation = useMutation({
    mutationFn: (d) => authApi.changePassword({ currentPassword: d.currentPassword, newPassword: d.newPassword }),
    onSuccess: () => { toast.success('Hasło zostało zmienione'); reset(); },
    onError: (e) => toast.error(e.response?.data?.message || 'Nieprawidłowe aktualne hasło'),
  });

  return (
    <div className="card">
      <SectionHeader icon={Key} title="Zmiana hasła" />
      <form onSubmit={handleSubmit(d => mutation.mutate(d))} noValidate className="space-y-4 max-w-md">
        <div>
          <label className="label">Aktualne hasło</label>
          <input {...register('currentPassword')} type="password" className={`input ${errors.currentPassword ? 'input-error' : ''}`} autoComplete="current-password" />
          {errors.currentPassword && <p className="error-msg">{errors.currentPassword.message}</p>}
        </div>
        <div>
          <label className="label">Nowe hasło</label>
          <input {...register('newPassword')} type="password" className={`input ${errors.newPassword ? 'input-error' : ''}`} autoComplete="new-password" />
          {errors.newPassword && <p className="error-msg">{errors.newPassword.message}</p>}
          <p className="text-xs text-gray-400 mt-1">Min. 8 znaków, wielka litera, cyfra, znak specjalny</p>
        </div>
        <div>
          <label className="label">Potwierdź nowe hasło</label>
          <input {...register('confirmPassword')} type="password" className={`input ${errors.confirmPassword ? 'input-error' : ''}`} autoComplete="new-password" />
          {errors.confirmPassword && <p className="error-msg">{errors.confirmPassword.message}</p>}
        </div>
        <button type="submit" className="btn-primary" disabled={mutation.isPending}>
          {mutation.isPending ? 'Zmienianie...' : 'Zmień hasło'}
        </button>
      </form>
    </div>
  );
}

// ── MFA section ────────────────────────────────────────────────────────────
function MfaSection() {
  const [setupOpen, setSetupOpen] = useState(false);
  const [qrData, setQrData] = useState(null);
  const [recoveryCodes, setRecoveryCodes] = useState(null);
  const [disableConfirm, setDisableConfirm] = useState(false);

  const { data: status, isLoading, refetch } = useQuery({
    queryKey: ['mfa-status'],
    queryFn: () => authApi.mfaStatus().then(r => r.data),
  });

  const startSetup = useMutation({
    mutationFn: () => authApi.mfaStart(),
    onSuccess: (r) => { setQrData(r.data); setSetupOpen(true); },
    onError: () => toast.error('Błąd inicjowania MFA'),
  });

  const { register, handleSubmit, reset, formState: { errors } } = useForm({
    resolver: zodResolver(mfaConfirmSchema),
  });

  const confirmSetup = useMutation({
    mutationFn: (d) => authApi.mfaConfirm({ code: d.code }),
    onSuccess: (r) => {
      toast.success('MFA włączone pomyślnie!');
      setRecoveryCodes(r.data.recoveryCodes);
      setSetupOpen(false);
      refetch();
      reset();
    },
    onError: () => toast.error('Nieprawidłowy kod weryfikacyjny'),
  });

  const disableMfa = useMutation({
    mutationFn: () => authApi.mfaDisable(),
    onSuccess: () => { toast.success('MFA wyłączone'); setDisableConfirm(false); refetch(); },
    onError: () => toast.error('Błąd wyłączania MFA'),
  });

  const regenerateCodes = useMutation({
    mutationFn: () => authApi.mfaRegenerateCodes(),
    onSuccess: (r) => { setRecoveryCodes(r.data.recoveryCodes); toast.success('Kody zastępcze wygenerowane'); },
    onError: () => toast.error('Błąd generowania kodów'),
  });

  if (isLoading) return <LoadingOverlay />;

  return (
    <div className="card">
      <SectionHeader icon={Smartphone} title="Weryfikacja dwuetapowa (MFA)" color={status?.mfaEnabled ? 'text-green-600' : 'text-gray-400'} />

      <div className="flex items-center justify-between mb-6 p-4 rounded-xl bg-gray-50">
        <div>
          <p className="font-medium text-gray-800">Status MFA</p>
          <p className="text-sm text-gray-500">{status?.mfaEnabled ? 'Aktywne – Twoje konto jest chronione' : 'Nieaktywne – zalecamy włączenie'}</p>
        </div>
        <span className={`badge ${status?.mfaEnabled ? 'badge-green' : 'badge-red'}`}>
          {status?.mfaEnabled ? 'Włączone' : 'Wyłączone'}
        </span>
      </div>

      <div className="flex flex-wrap gap-3">
        {!status?.mfaEnabled ? (
          <button className="btn-primary" onClick={() => startSetup.mutate()} disabled={startSetup.isPending}>
            <Shield size={15} /> Włącz MFA
          </button>
        ) : (
          <>
            <button className="btn-secondary" onClick={() => regenerateCodes.mutate()} disabled={regenerateCodes.isPending}>
              <RefreshCw size={15} /> Wygeneruj kody zastępcze
            </button>
            <button className="btn-danger" onClick={() => setDisableConfirm(true)}>
              Wyłącz MFA
            </button>
          </>
        )}
      </div>

      {/* Recovery codes display */}
      {recoveryCodes && (
        <div className="mt-4 p-4 bg-yellow-50 border border-yellow-200 rounded-xl">
          <p className="text-sm font-semibold text-yellow-800 mb-2">⚠️ Zapisz kody zastępcze w bezpiecznym miejscu!</p>
          <div className="grid grid-cols-2 gap-2">
            {recoveryCodes.map((c, i) => (
              <div key={i} className="font-mono text-sm bg-white rounded px-2 py-1 border border-yellow-200">{c}</div>
            ))}
          </div>
        </div>
      )}

      {/* MFA Setup modal */}
      <Modal open={setupOpen} onClose={() => setSetupOpen(false)} title="Konfiguracja MFA" size="sm">
        {qrData && (
          <div className="space-y-4 text-center">
            <p className="text-sm text-gray-600">Zeskanuj kod QR aplikacją uwierzytelniającą (Google Authenticator, Authy, itp.)</p>
            <div className="flex justify-center">
              <QRCodeSVG value={qrData.qrUri || qrData.otpAuthUrl || ''} size={180} />
            </div>
            {qrData.secret && (
              <div>
                <p className="text-xs text-gray-400 mb-1">Lub wprowadź ręcznie klucz:</p>
                <code className="text-xs bg-gray-100 rounded px-2 py-1 break-all">{qrData.secret}</code>
              </div>
            )}
            <form onSubmit={handleSubmit(d => confirmSetup.mutate(d))} noValidate className="space-y-3">
              <div>
                <label className="label">Kod weryfikacyjny (6 cyfr)</label>
                <input {...register('code')} type="text" inputMode="numeric" maxLength={6}
                  className={`input text-center tracking-widest text-xl font-mono ${errors.code ? 'input-error' : ''}`}
                  placeholder="000000" autoFocus autoComplete="one-time-code" />
                {errors.code && <p className="error-msg">{errors.code.message}</p>}
              </div>
              <button type="submit" className="btn-primary w-full justify-center" disabled={confirmSetup.isPending}>
                {confirmSetup.isPending ? 'Weryfikowanie...' : 'Potwierdź i włącz MFA'}
              </button>
            </form>
          </div>
        )}
      </Modal>

      <ConfirmModal open={disableConfirm} onClose={() => setDisableConfirm(false)}
        onConfirm={() => disableMfa.mutate()}
        title="Wyłącz MFA" message="Czy na pewno chcesz wyłączyć weryfikację dwuetapową? Spowoduje to obniżenie bezpieczeństwa konta." danger />
    </div>
  );
}

// ── Sessions section ───────────────────────────────────────────────────────
function SessionsSection() {
  const qc = useQueryClient();
  const { logout } = useAuth();
  const [revokeId, setRevokeId] = useState(null);
  const [revokeAllConfirm, setRevokeAllConfirm] = useState(false);

  const { data: sessions, isLoading } = useQuery({
    queryKey: ['sessions'],
    queryFn: () => authApi.sessions().then(r => r.data),
  });

  const revokeMutation = useMutation({
    mutationFn: (id) => authApi.revokeSession(id),
    onSuccess: async () => {
      const updated = await qc.invalidateQueries(['sessions']);
      setRevokeId(null);
      toast.success('Sesja zakończona');
      // Po usunięciu sesji odśwież – jeśli nie ma już aktywnych, wyloguj
      const remaining = qc.getQueryData(['sessions']);
      if (!remaining || remaining.length === 0) {
        toast.info('Wszystkie sesje zakończone. Wylogowywanie...');
        setTimeout(() => logout(), 1500);
      }
    },
    onError: () => toast.error('Błąd kończenia sesji'),
  });

  const revokeAllMutation = useMutation({
    mutationFn: async () => {
      const list = sessions || [];
      for (const s of list) {
        await authApi.revokeSession(s.id);
      }
    },
    onSuccess: () => {
      setRevokeAllConfirm(false);
      toast.success('Wszystkie sesje zakończone. Wylogowywanie...');
      setTimeout(() => logout(), 1500);
    },
    onError: () => toast.error('Błąd kończenia sesji'),
  });

  return (
    <div className="card">
      <div className="flex items-center justify-between mb-4">
        <SectionHeader icon={Monitor} title="Aktywne sesje" />
        {sessions?.length > 0 && (
          <button className="btn-danger text-xs py-1" onClick={() => setRevokeAllConfirm(true)}>
            <LogOut size={13} /> Zakończ wszystkie
          </button>
        )}
      </div>
      {isLoading ? <LoadingOverlay /> : (
        <div className="space-y-2">
          {sessions?.length === 0 ? (
            <p className="text-sm text-gray-400">Brak aktywnych sesji</p>
          ) : (
            sessions?.map((s) => (
              <div key={s.id} className="flex items-center justify-between p-3 rounded-lg bg-gray-50">
                <div>
                  <p className="text-sm font-medium text-gray-800">
                    {s.userAgent ? s.userAgent.substring(0, 60) + (s.userAgent.length > 60 ? '...' : '') : 'Nieznana przeglądarka'}
                  </p>
                  <p className="text-xs text-gray-500">
                    IP: {s.ip || s.ipAddress || '—'} •{' '}
                    {s.issuedAt ? format(new Date(s.issuedAt), 'dd.MM.yyyy HH:mm', { locale: pl }) : ''}
                  </p>
                </div>
                <button className="btn-ghost p-1.5 text-red-500 hover:text-red-700" onClick={() => setRevokeId(s.id)} title="Zakończ sesję">
                  <Trash2 size={15} />
                </button>
              </div>
            ))
          )}
        </div>
      )}
      <ConfirmModal open={!!revokeId} onClose={() => setRevokeId(null)}
        onConfirm={() => revokeMutation.mutate(revokeId)}
        title="Zakończ sesję" message="Czy na pewno chcesz zakończyć tę sesję? Jeśli to bieżąca sesja, zostaniesz wylogowany." danger />
      <ConfirmModal open={revokeAllConfirm} onClose={() => setRevokeAllConfirm(false)}
        onConfirm={() => revokeAllMutation.mutate()}
        title="Zakończ wszystkie sesje" message="Czy na pewno chcesz zakończyć WSZYSTKIE aktywne sesje? Zostaniesz natychmiast wylogowany." danger />
    </div>
  );
}

// ── Main page ──────────────────────────────────────────────────────────────
export default function ProfilePage() {
  const { user } = useAuth();

  return (
    <div className="space-y-6">
      <div>
        <h1 className="page-title">Profil i bezpieczeństwo</h1>
        <p className="text-sm text-gray-500 mt-1">Zarządzaj ustawieniami konta i bezpieczeństwem</p>
      </div>

      {/* User info card */}
      <div className="card">
        <div className="flex items-center gap-4">
          <div className="w-16 h-16 rounded-2xl bg-primary-100 flex items-center justify-center text-primary-700 text-2xl font-bold">
            {user?.username?.[0]?.toUpperCase() || 'U'}
          </div>
          <div>
            <h2 className="text-xl font-semibold text-gray-800">{user?.username}</h2>
            <p className="text-sm text-gray-500 capitalize">{user?.role?.toLowerCase()}</p>
            {user?.email && <p className="text-sm text-gray-400">{user.email}</p>}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
        <ChangePasswordSection />
        <MfaSection />
      </div>
      <SessionsSection />
    </div>
  );
}
