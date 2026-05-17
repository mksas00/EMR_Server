# EMR System – Frontend

React 18 + Vite + Tailwind CSS frontend dla systemu zarządzania elektronioczną dokumentacją medyczną.

## Stack technologiczny

| Biblioteka | Zastosowanie |
|---|---|
| **React 18** | UI framework |
| **Vite 8** | Build tool / dev server |
| **React Router v6** | Client-side routing |
| **TanStack Query v5** | Zarządzanie stanem serwera / cache |
| **React Hook Form + Zod** | Formularze + walidacja |
| **Tailwind CSS v3** | Utility-first styling |
| **Headless UI** | Dostępne komponenty (Modal, Dialog) |
| **Lucide React** | Ikony |
| **Axios** | HTTP client z interceptorami JWT |
| **React Toastify** | Powiadomienia |
| **date-fns** | Formatowanie dat |
| **qrcode.react** | Generowanie QR kodów (MFA setup) |

## Uruchomienie

```bash
cd client
cp .env.example .env      # edytuj jeśli backend nie jest na :8080
npm install
npm run dev               # http://localhost:3000
```

## Produkcja

```bash
npm run build             # wyniki w dist/
npm run preview           # podgląd buildu
```

## Struktura katalogów

```
src/
├── api/
│   ├── axiosClient.js    # Axios + interceptory JWT (auto-refresh)
│   └── index.js          # Wszystkie API endpoints
├── components/
│   ├── Layout.jsx         # Sidebar + Topbar
│   ├── Modal.jsx          # Modal + ConfirmModal
│   ├── Pagination.jsx     # Paginacja
│   ├── ProtectedRoute.jsx # Guard routingu z RBAC
│   └── Spinner.jsx        # Loading spinner
├── contexts/
│   └── AuthContext.jsx    # Auth state, login/logout
└── pages/
    ├── auth/
    │   └── LoginPage.jsx  # Login + MFA challenge
    ├── DashboardPage.jsx  # Dashboard ze statystykami
    ├── patients/
    │   ├── PatientsPage.jsx
    │   └── PatientDetailPage.jsx
    ├── visits/
    │   └── VisitsPage.jsx
    ├── prescriptions/
    │   └── PrescriptionsPage.jsx
    ├── lab/
    │   └── LabResultsPage.jsx
    ├── records/
    │   └── MedicalRecordsPage.jsx
    └── profile/
        └── ProfilePage.jsx  # Zmiana hasła + MFA + Sesje
```

## Bezpieczeństwo zaimplementowane we frontendzie

- **JWT auto-refresh** – interceptor Axios automatycznie odświeża token przy 401
- **Protected Routes z RBAC** – `<ProtectedRoute roles={['admin','doctor']}>` blokuje dostęp nieuprawnionym
- **Zod walidacja** – wszystkie formularze walidowane client-side (PESEL 11 cyfr, UUID, siła hasła)
- **Bezpieczne hasło** – wymóg  ≥8 znaków, wielka litera, cyfra, znak specjalny
- **TOTP MFA** – pełny flow: start → QR code → weryfikacja → kody recovery
- **Content Security Policy** – ustawiony meta CSP w `index.html`
- **noValidate + Zod** – blokowanie natywnej walidacji przeglądarki, obsługa po stronie JS
- **Confirm modals** – wymagane potwierdzenie dla operacji nieodwracalnych (delete)
- **Auto-logout** – event `auth:logout` odpina użytkownika przy wygaśnięciu tokenów
- **Brak wrażliwych danych w cache** – `staleTime: 30s`, nie persist query cache

