const UID_KEY = "uid";
const ROLE_KEY = "role";
const TOKEN_KEY = "token";

export function isLoggedIn(){ return !!localStorage.getItem(UID_KEY); }
export function getRole(){ return localStorage.getItem(ROLE_KEY) || "USER"; }
export function getToken(){ return localStorage.getItem(TOKEN_KEY); }
export function landingRouteFor(role){
  switch ((role || "").toUpperCase()) {
    case "ADMIN": return "/admin";
    default: return "/lost";
  }
}
export function setSession({ id, role, token }){
  localStorage.setItem(UID_KEY, id);
  if (role) {
    localStorage.setItem(ROLE_KEY, role);
  } else {
    localStorage.removeItem(ROLE_KEY);
  }
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}
export function logout(){
  localStorage.removeItem(UID_KEY);
  localStorage.removeItem(ROLE_KEY);
  localStorage.removeItem(TOKEN_KEY);
  window.location.href = "/";
}
