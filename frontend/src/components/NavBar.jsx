import React from "react";
import { Link } from "react-router-dom";

export default function NavBar() {
  const s = { marginRight: 12 };
  return (
    <nav style={{ padding: 16, borderBottom: "1px solid #ddd" }}>
      <Link to="/" style={s}>Login</Link>
      <Link to="/lost" style={s}>Report Lost</Link>
      <Link to="/found" style={s}>Report Found</Link>
    </nav>
  );
}
