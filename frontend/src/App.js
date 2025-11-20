import React from "react";
import { Routes, Route, Navigate, useLocation } from "react-router-dom";
import NavBar from "./components/NavBar";
import Login from "./pages/Login";
import Lost from "./pages/Lost";
import Found from "./pages/Found";
import LostReports from "./pages/LostReports";
import FoundReports from "./pages/FoundReports";
import AdminDashboard from "./pages/AdminDashboard";
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
  const { pathname } = useLocation();
  return (
    <>
      {pathname !== "/" && <NavBar />}
      <div className="container">
        <Routes>
          <Route path="/" element={<Login />} />
          <Route path="/lost" element={<Private roles={["USER", "ADMIN"]}><Lost /></Private>} />
          <Route path="/found" element={<Private roles={["ADMIN"]}><Found /></Private>} />
          <Route path="/lost-reports" element={<Private roles={["USER", "ADMIN"]}><LostReports /></Private>} />
          <Route path="/found-reports" element={<Private roles={["ADMIN"]}><FoundReports /></Private>} />
          <Route path="/admin" element={<Private roles={["ADMIN"]}><AdminDashboard /></Private>} />
          <Route path="*" element={<Navigate to={isLoggedIn() ? landingRouteFor(getRole()) : "/"} replace />} />
        </Routes>
      </div>
    </>
  );
}