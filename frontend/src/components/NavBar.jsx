import React from "react";
import { Link, useLocation } from "react-router-dom";
import { isLoggedIn, logout, getRole } from "../auth";

export default function NavBar() {
  const logged = isLoggedIn();
  const role = getRole();
  const { pathname } = useLocation();
  return (
    <nav className="nav card" style={{margin:'16px auto',maxWidth:1100}}>
      <div style={{fontWeight:700}}>NEIU Lost & Found</div>
      <div className="spacer" />
      {!logged && pathname !== "/" && <Link className="link" to="/">Login</Link>}
      {logged && (
        <>
          {(role === "USER" || role === "ADMIN") && (
            <Link to="/lost" className={pathname==="/lost"?"link":""}>Report Lost Item</Link>
          )}
          {role === "ADMIN" && (
            <Link to="/found" className={pathname==="/found"?"link":""}>Report Found Item</Link>
          )}
          {role === "USER" && (
            <Link to="/lost-reports" className={pathname==="/lost-reports"?"link":""}>My Lost Items</Link>
          )}
          {role === "ADMIN" && (
            <Link to="/lost-reports" className={pathname==="/lost-reports"?"link":""}>Lost Items</Link>
          )}
          {role === "ADMIN" && (
            <Link to="/found-reports" className={pathname==="/found-reports"?"link":""}>Found Items</Link>
          )}
          {role === "ADMIN" && (
            <Link to="/admin" className={pathname==="/admin"?"link":""}>Handoff Queue</Link>
          )}
          <button className="btn" onClick={logout} style={{padding:'8px 14px'}}>Logout</button>
        </>
      )}
    </nav>
  );
}
