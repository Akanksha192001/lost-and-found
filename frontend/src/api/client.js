const BASE = (process.env.REACT_APP_API_URL || "").replace(/\/$/, "");
export async function api(path, options = {}) {
  // Prevent manual OPTIONS calls
  if ((options.method || '').toUpperCase() === 'OPTIONS') {
    throw new Error('OPTIONS method is not allowed from client');
  }
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  
  const res = await fetch(`${BASE}${path.startsWith("/") ? path : `/${path}`}`, {
    ...options,
    headers,
    credentials: "include" // Include session cookies with requests
  });
  
  if (!res.ok) {
    let errorData = null;
    let errorMsg = res.statusText;
    
    try {
      const contentType = res.headers.get('content-type');
      if (contentType && contentType.includes('application/json')) {
        errorData = await res.json();
        errorMsg = errorData.message || errorData.error || JSON.stringify(errorData);
      } else {
        errorMsg = await res.text();
      }
    } catch (e) {
      // fallback to statusText
    }
    
    // Handle authentication/authorization errors
    if (res.status === 401) {
      // Clear local session data
      localStorage.removeItem("uid");
      localStorage.removeItem("role");
      localStorage.removeItem("userName");
      localStorage.removeItem("userEmail");
      
      // Create a more specific error for authentication
      const authError = new Error(errorMsg || "Session expired. Please login again.");
      authError.status = 401;
      authError.isAuthError = true;
      throw authError;
    }
    
    if (res.status === 403) {
      const forbiddenError = new Error(errorMsg || "Access denied. Insufficient privileges.");
      forbiddenError.status = 403;
      forbiddenError.isForbidden = true;
      throw forbiddenError;
    }
    
    const error = new Error(errorMsg || `Error ${res.status}`);
    error.status = res.status;
    throw error;
  }
  
  // Handle 204 No Content responses (e.g., DELETE operations)
  if (res.status === 204) {
    return null;
  }
  
  // Check if response has content before parsing JSON
  const contentType = res.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    return res.json();
  }
  
  // Return null for empty responses
  return null;
}
