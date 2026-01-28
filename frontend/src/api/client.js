const accessTokenKey = 'accessToken';
const refreshTokenKey = 'refreshToken';

const getAccessToken = () => localStorage.getItem(accessTokenKey);
const getRefreshToken = () => localStorage.getItem(refreshTokenKey);

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

const refreshSession = async () => {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    throw new Error('refresh token ausente');
  }
  const response = await fetch('/api/auth/refresh', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ refreshToken })
  });
  const data = await handleResponse(response);
  storeSession(data);
  return data;
};

const request = async (method, path, body, options = {}) => {
  const config = {
    method,
    headers: buildHeaders({ ...options, method, body }),
    body: options.isForm ? body : body !== undefined ? JSON.stringify(body) : undefined
  };
  const response = await fetch(path, config);
  if (response.status === 401 && !options.skipRefresh && !options._retry) {
    try {
      await refreshSession();
      return request(method, path, body, { ...options, _retry: true });
    } catch (error) {
      clearSession();
      window.location.assign('/login');
      throw error;
    }
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
