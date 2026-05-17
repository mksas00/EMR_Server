import axiosClient from './axiosClient';

export const authApi = {
  login: (data) => axiosClient.post('/auth/login', data),
  refresh: (data) => axiosClient.post('/auth/refresh', data),
  logout: (refreshToken) => axiosClient.post('/auth/logout', { refreshToken }),
  sessions: () => axiosClient.get('/auth/sessions'),
  revokeSession: (id) => axiosClient.delete(`/auth/sessions/${id}`),
  changePassword: (data) => axiosClient.post('/auth/change-password', data),
  mfaStatus: () => axiosClient.get('/auth/mfa/status'),
  mfaStart: () => axiosClient.post('/auth/mfa/start'),
  mfaConfirm: (data) => axiosClient.post('/auth/mfa/confirm', data),
  mfaDisable: () => axiosClient.post('/auth/mfa/disable'),
  mfaRegenerateCodes: () => axiosClient.post('/auth/mfa/recovery/regenerate'),
  passwordResetRequest: (data) => axiosClient.post('/auth/password-reset/request', data),
  passwordResetConfirm: (data) => axiosClient.post('/auth/password-reset/confirm', data),
};

const buildParams = (filters) => {
  const p = new URLSearchParams();
  Object.entries(filters).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') p.append(k, v);
  });
  return p;
};

// ── Patients ─────────────────────────────────────────────────────────────
export const patientApi = {
  list: (filters = {}, pageable = {}) =>
    axiosClient.get('/api/pacjenci', { params: { ...filters, ...pageable } }),
  get: (id) => axiosClient.get(`/api/pacjenci/${id}`),
  create: (data) => axiosClient.post('/api/pacjenci', data),
  update: (id, data) => axiosClient.put(`/api/pacjenci/${id}`, data),
  remove: (id) => axiosClient.delete(`/api/pacjenci/${id}`),
};

// ── Visits ────────────────────────────────────────────────────────────────
export const visitApi = {
  list: (filters = {}, pageable = {}) =>
    axiosClient.get('/api/wizyty', { params: { ...filters, ...pageable } }),
  get: (id) => axiosClient.get(`/api/wizyty/${id}`),
  create: (patientId, data) => axiosClient.post(`/api/wizyty/${patientId}`, data),
  update: (id, data) => axiosClient.put(`/api/wizyty/${id}`, data),
  remove: (id) => axiosClient.delete(`/api/wizyty/${id}`),
  downloadIcs: (id) => axiosClient.get(`/api/wizyty/${id}/ics`, { responseType: 'blob' }),
};

// ── Prescriptions ─────────────────────────────────────────────────────────
export const prescriptionApi = {
  list: (filters = {}, pageable = {}) =>
    axiosClient.get('/api/recepty', { params: { ...filters, ...pageable } }),
  get: (id) => axiosClient.get(`/api/recepty/${id}`),
  create: (data) => axiosClient.post('/api/recepty', data),
  update: (id, data) => axiosClient.put(`/api/recepty/${id}`, data),
  remove: (id) => axiosClient.delete(`/api/recepty/${id}`),
  downloadPdf: (id) => axiosClient.get(`/api/recepty/${id}/pdf`, { responseType: 'blob' }),
};

// ── Lab Results ───────────────────────────────────────────────────────────
export const labResultApi = {
  list: (filters = {}, pageable = {}) =>
    axiosClient.get('/api/wyniki-lab', { params: { ...filters, ...pageable } }),
  get: (id) => axiosClient.get(`/api/wyniki-lab/${id}`),
  create: (data) => axiosClient.post('/api/wyniki-lab', data),
  update: (id, data) => axiosClient.put(`/api/wyniki-lab/${id}`, data),
  remove: (id) => axiosClient.delete(`/api/wyniki-lab/${id}`),
};

// ── Patient Consents ──────────────────────────────────────────────────
export const consentApi = {
  grant: (data) => axiosClient.post('/api/consents', data),
  revoke: (id) => axiosClient.delete(`/api/consents/${id}`),
  listForPatient: (patientId, pageable = {}) =>
    axiosClient.get(`/api/consents/patient/${patientId}`, { params: pageable }),
  listMine: (pageable = {}) =>
    axiosClient.get('/api/consents/mine', { params: pageable }),
};

// ── Lab Orders (nowy moduł) ──────────────────────────────────────────────
export const labOrderApi = {
  list: (filters = {}, pageable = {}) =>
    axiosClient.get('/api/lab-orders', { params: { ...filters, ...pageable } }),
  get: (id) => axiosClient.get(`/api/lab-orders/${id}`),
  create: (data) => axiosClient.post('/api/lab-orders', data),
  cancel: (id, reason) => axiosClient.post(`/api/lab-orders/${id}/cancel`, { reason }),
  updateItem: (itemId, data) => axiosClient.patch(`/api/lab-orders/items/${itemId}`, data),
};

export const labPanelApi = {
  list: () => axiosClient.get('/api/lab-panels'),
};

// ── Allergies ─────────────────────────────────────────────────────────────
export const allergyApi = {
  list: (filters = {}, pageable = {}) =>
    axiosClient.get('/api/alergie', { params: { ...filters, ...pageable } }),
  get: (id) => axiosClient.get(`/api/alergie/${id}`),
  create: (data) => axiosClient.post('/api/alergie', data),
  update: (id, data) => axiosClient.put(`/api/alergie/${id}`, data),
  remove: (id) => axiosClient.delete(`/api/alergie/${id}`),
};

// ── Chronic Diseases ──────────────────────────────────────────────────────
export const chronicDiseaseApi = {
  list: (filters = {}, pageable = {}) =>
    axiosClient.get('/api/choroby-przewlekle', { params: { ...filters, ...pageable } }),
  get: (id) => axiosClient.get(`/api/choroby-przewlekle/${id}`),
  create: (data) => axiosClient.post('/api/choroby-przewlekle', data),
  update: (id, data) => axiosClient.put(`/api/choroby-przewlekle/${id}`, data),
  remove: (id) => axiosClient.delete(`/api/choroby-przewlekle/${id}`),
};


// ── Medical Files ─────────────────────────────────────────────────────────
export const medicalFileApi = {
  list: (filters = {}, pageable = {}) =>
    axiosClient.get('/api/pliki-medyczne', { params: { ...filters, ...pageable } }),
  get: (id) => axiosClient.get(`/api/pliki-medyczne/${id}`),
  create: (data) => axiosClient.post('/api/pliki-medyczne', data),
  remove: (id) => axiosClient.delete(`/api/pliki-medyczne/${id}`),
};

// ── Visit Slots ───────────────────────────────────────────────────────────
export const visitSlotApi = {
  list: (filters = {}) =>
    axiosClient.get('/api/sloty', { params: filters }),
  create: (data) => axiosClient.post('/api/sloty', data),
  reserve: (slotId, patientId) =>
    axiosClient.post(`/api/sloty/${slotId}/reserve`, { patientId }),
  release: (slotId) => axiosClient.post(`/api/sloty/${slotId}/release`),
  remove: (slotId) => axiosClient.delete(`/api/sloty/${slotId}`),
};

// ── Medications ───────────────────────────────────────────────────────────
export const medicationApi = {
  search: (params = {}) => axiosClient.get('/api/leki', { params }),
  get: (id) => axiosClient.get(`/api/leki/${id}`),
  findByGtin: (gtin) => axiosClient.get('/api/leki/opakowania', { params: { gtin } }),
};

// ── Admin ─────────────────────────────────────────────────────────────────
export const adminApi = {
  dashboard: (params) => axiosClient.get('/api/admin/dashboard', { params }),
  auditLog: (params = {}) => axiosClient.get('/api/admin/audit-log', { params }),
  incidents: (params = {}) => axiosClient.get('/api/admin/incidents', { params }),
  updateIncidentStatus: (id, status) =>
    axiosClient.patch(`/api/admin/incidents/${id}/status`, { status }),
  loginAttempts: (params = {}) => axiosClient.get('/api/admin/login-attempts', { params }),
  sessions: (params = {}) => axiosClient.get('/api/admin/sessions', { params }),
  revokeSession: (id) => axiosClient.delete(`/api/admin/sessions/${id}`),
  users: (params = {}) => axiosClient.get('/api/admin/users', { params }),
  unlockUser: (id) => axiosClient.post(`/api/admin/users/${id}/unlock`),
  lockUser: (id) => axiosClient.post(`/api/admin/users/${id}/lock`),
};
