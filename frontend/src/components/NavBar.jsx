import React, { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { isLoggedIn, logout, getRole } from "../auth";

export default function NavBar() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const logged = isLoggedIn();
  const role = getRole();
  const { pathname } = useLocation();
  
  const handleLinkClick = () => {
    setMobileMenuOpen(false);
  };
  
  return (
    <nav className="nav card" style={{margin:'16px auto',maxWidth:1100}}>
      <div style={{fontWeight:700}}>NEIU Lost & Found</div>
      <div className="spacer" />
      
      {/* Mobile Menu Toggle */}
      <button 
        className="mobile-nav-toggle"
        onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
        aria-label="Toggle navigation"
      >
        <span className={`nav-hamburger ${mobileMenuOpen ? 'open' : ''}`}></span>
      </button>
      
      {/* Navigation Links */}
      <div className={`nav-menu ${mobileMenuOpen ? 'mobile-open' : ''}`}>
        {!logged && pathname !== "/" && (
          <Link className="link" to="/" onClick={handleLinkClick}>Login</Link>
        )}
        {logged && (
          <>
            {(role === "USER" || role === "ADMIN") && (
              <Link to="/lost" className={pathname==="/lost"?"link":""} onClick={handleLinkClick}>Report Lost Item</Link>
            )}
            {role === "ADMIN" && (
              <Link to="/found" className={pathname==="/found"?"link":""} onClick={handleLinkClick}>Report Found Item</Link>
            )}
            {role === "USER" && (
              <Link to="/lost-reports" className={pathname==="/lost-reports"?"link":""} onClick={handleLinkClick}>Lost Items</Link>
            )}
            {role === "ADMIN" && (
              <Link to="/lost-reports" className={pathname==="/lost-reports"?"link":""} onClick={handleLinkClick}>Lost Items</Link>
            )}
            {role === "ADMIN" && (
              <Link to="/found-reports" className={pathname==="/found-reports"?"link":""} onClick={handleLinkClick}>Found Items</Link>
            )}
            {role === "ADMIN" && (
              <Link to="/admin" className={pathname==="/admin"?"link":""} onClick={handleLinkClick}>Handoff Queue</Link>
            )}
            <button className="btn" onClick={() => { logout(); handleLinkClick(); }} style={{padding:'8px 14px'}}>Logout</button>
          </>
        )}
      </div>
    </nav>
  );
}
