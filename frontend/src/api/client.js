const BASE = (process.env.REACT_APP_API_URL || "").replace(/\/$/, "");
export async function api(path, options = {}) {
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  const token = localStorage.getItem("token");
  if (token && !headers.Authorization) {
    headers.Authorization = `Bearer ${token}`;
  }
  const res = await fetch(`${BASE}${path.startsWith("/") ? path : `/${path}`}`, {
    ...options,
    headers
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || res.statusText);
  }
  return res.json();
}
