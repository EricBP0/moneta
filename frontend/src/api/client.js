const accessTokenKey = 'accessToken';
const refreshTokenKey = 'refreshToken';

const getAccessToken = () => localStorage.getItem(accessTokenKey);

/**
 * Stores authentication tokens in localStorage.
 * 
 * Security Note: localStorage is vulnerable to XSS attacks. Tokens stored here can be
 * accessed by any JavaScript code running in the same origin. Consider the following:
 * - This is a common pattern for SPAs and is acceptable for many applications
 * - For enhanced security, httpOnly cookies for refresh tokens are recommended if supported by backend
 * - Ensure proper Content Security Policy (CSP) headers are in place
 * - Keep tokens short-lived and implement proper refresh token rotation
 */
export const storeSession = (authResponse) => {
  if (!authResponse) {
    return;
  }
  localStorage.setItem(accessTokenKey, authResponse.accessToken);
  localStorage.setItem(refreshTokenKey, authResponse.refreshToken);
};

export const clearSession = () => {
  localStorage.removeItem(accessTokenKey);
  localStorage.removeItem(refreshTokenKey);
  localStorage.removeItem('user');
};

const buildHeaders = (options = {}) => {
  const headers = { ...options.headers };
  if (!options.skipAuth) {
    const token = getAccessToken();
    if (token) {
      headers.Authorization = `Bearer ${token}`;
    }
  }
  if (!options.isForm && options.method !== 'GET' && options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  return headers;
};

const handleResponse = async (response) => {
  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || 'Erro na requisição');
  }
  if (response.status === 204) {
    return null;
  }
  return response.json();
};

const request = async (method, path, body, options = {}) => {
  const config = {
    method,
    headers: buildHeaders({ ...options, method, body }),
    body: options.isForm ? body : body !== undefined ? JSON.stringify(body) : undefined
  };
  const response = await fetch(path, config);
  if (response.status === 401) {
    clearSession();
    if (window.location.pathname !== '/login') {
      window.location.assign('/login');
    }
    throw new Error('Sessão expirada. Faça login novamente.');
  }
  return handleResponse(response);
};

export const apiClient = {
  get(path, options = {}) {
    return request('GET', path, undefined, options);
  },
  post(path, body, options = {}) {
    return request('POST', path, body, options);
  },
  patch(path, body, options = {}) {
    return request('PATCH', path, body, options);
  },
  delete(path, options = {}) {
    return request('DELETE', path, undefined, options);
  }
};
