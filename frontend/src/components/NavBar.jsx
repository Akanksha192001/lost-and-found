import React from "react";
import { Link, useLocation } from "react-router-dom";
import { isLoggedIn, logout } from "../auth";

export default function NavBar() {
  const logged = isLoggedIn();
  const { pathname } = useLocation();
  return (
    <nav className="nav card" style={{margin:'16px auto',maxWidth:1100}}>
      <div style={{fontWeight:700}}>NEIU Lost & Found</div>
      <div className="spacer" />
      {!logged && pathname !== "/" && <Link className="link" to="/">Login</Link>}
      {logged && (
        <>
          <Link to="/lost" className={pathname==="/lost"?"link":""}>Report Lost</Link>
          <Link to="/found" className={pathname==="/found"?"link":""}>Report Found</Link>
          <button className="btn" onClick={logout} style={{padding:'8px 14px'}}>Logout</button>
        </>
      )}
    </nav>
  );
}
