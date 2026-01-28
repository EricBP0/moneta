const defaultHeaders = () => {
  const token = localStorage.getItem('accessToken');
  return token ? { Authorization: `Bearer ${token}` } : {};
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

export const apiClient = {
  async get(path) {
    const response = await fetch(path, {
      headers: {
        ...defaultHeaders()
      }
    });
    return handleResponse(response);
  },
  async post(path, body, options = {}) {
    const headers = { ...defaultHeaders(), ...options.headers };
    if (!options.isForm) {
      headers['Content-Type'] = 'application/json';
    }
    const response = await fetch(path, {
      method: 'POST',
      headers,
      body: options.isForm ? body : JSON.stringify(body)
    });
    return handleResponse(response);
  },
  async patch(path, body) {
    const response = await fetch(path, {
      method: 'PATCH',
      headers: {
        ...defaultHeaders(),
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(body)
    });
    return handleResponse(response);
  },
  async delete(path) {
    const response = await fetch(path, {
      method: 'DELETE',
      headers: {
        ...defaultHeaders()
      }
    });
    return handleResponse(response);
  }
};
