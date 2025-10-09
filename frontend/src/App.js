import React from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import NavBar from "./components/NavBar";
import Login from "./pages/Login";
import Lost from "./pages/Lost";
import Found from "./pages/Found";
import Admin from "./pages/Admin";
import Security from "./pages/Security";
import { isLoggedIn, getRole, landingRouteFor } from "./auth";

function Private({ children, roles }) {
  if (!isLoggedIn()) {
    return <Navigate to="/" replace />;
  }
  const currentRole = getRole();
  if (roles && !roles.includes(currentRole)) {
    return <Navigate to={landingRouteFor(currentRole)} replace />;
  }
  return children;
}

export default function App() {
  return (
    <>
      <NavBar />
      <div className="container">
        <Routes>
          <Route path="/" element={<Login />} />
          <Route path="/lost" element={<Private roles={["STUDENT"]}><Lost /></Private>} />
          <Route path="/found" element={<Private roles={["STUDENT", "ADMIN"]}><Found /></Private>} />
          <Route path="/admin" element={<Private roles={["ADMIN"]}><Admin /></Private>} />
          <Route path="/handoff" element={<Private roles={["SECURITY"]}><Security /></Private>} />
          <Route path="*" element={<Navigate to={isLoggedIn() ? landingRouteFor(getRole()) : "/"} replace />} />
        </Routes>
      </div>
    </>
  );
}
