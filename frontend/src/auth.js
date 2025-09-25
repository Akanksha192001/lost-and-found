export function isLoggedIn(){ return !!localStorage.getItem("uid"); }
export function logout(){ localStorage.removeItem("uid"); window.location.href = "/"; }
