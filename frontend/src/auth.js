const UID_KEY = "uid";
const ROLE_KEY = "role";
const USER_NAME_KEY = "userName";
const USER_EMAIL_KEY = "userEmail";

export function isLoggedIn(){ return !!localStorage.getItem(UID_KEY); }
export function getRole(){ return localStorage.getItem(ROLE_KEY) || "USER"; }
export function getUserName(){ return localStorage.getItem(USER_NAME_KEY) || ""; }
export function getUserEmail(){ return localStorage.getItem(USER_EMAIL_KEY) || ""; }
export function landingRouteFor(role){
  switch ((role || "").toUpperCase()) {
    case "ADMIN": return "/admin";
    default: return "/lost";
  }
}
export function setSession({ id, role, name, email }){
  localStorage.setItem(UID_KEY, id);
  if (role) {
    localStorage.setItem(ROLE_KEY, role);
  } else {
    localStorage.removeItem(ROLE_KEY);
  }
  if (name) {
    localStorage.setItem(USER_NAME_KEY, name);
  } else {
    localStorage.removeItem(USER_NAME_KEY);
  }
  if (email) {
    localStorage.setItem(USER_EMAIL_KEY, email);
  } else {
    localStorage.removeItem(USER_EMAIL_KEY);
  }
}
export function logout(){
  localStorage.removeItem(UID_KEY);
  localStorage.removeItem(ROLE_KEY);
  localStorage.removeItem(USER_NAME_KEY);
  localStorage.removeItem(USER_EMAIL_KEY);
  // Call logout endpoint to clear session cookie
  fetch("/auth/logout", { 
    method: "POST", 
    credentials: "include" 
  }).finally(() => {
    window.location.href = "/";
  });
}

// Utility function to clear session and redirect to login
export function handleSessionExpiry() {
  // Clear local session data
  localStorage.removeItem(UID_KEY);
  localStorage.removeItem(ROLE_KEY);
  localStorage.removeItem(USER_NAME_KEY);
  localStorage.removeItem(USER_EMAIL_KEY);
  
  // Show user-friendly message
  alert("Your session has expired. Please login again.");
  
  // Redirect to login page
  window.location.href = "/";
}

// Utility function to handle authentication errors in components
export function handleAuthError(error, navigate) {
  if (error.isAuthError || error.status === 401) {
    handleSessionExpiry();
    return true; // Indicates auth error was handled
  }
  return false; // Not an auth error
}
