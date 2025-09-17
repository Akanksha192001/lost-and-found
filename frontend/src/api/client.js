const BASE = (process.env.REACT_APP_API_URL || "").replace(/\/$/, "");
export async function api(path, options = {}) {
  const res = await fetch(`${BASE}${path.startsWith("/") ? path : `/${path}`}`, {
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || res.statusText);
  }
  return res.json();
}
