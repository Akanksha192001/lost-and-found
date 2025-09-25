import React from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import NavBar from "./components/NavBar";
import Login from "./pages/Login";
import Lost from "./pages/Lost";
import Found from "./pages/Found";
import { isLoggedIn } from "./auth";

function Private({ children }) {
  return isLoggedIn() ? children : <Navigate to="/" replace />;
}

export default function App() {
  return (
    <>
      <NavBar />
      <div className="container">
        <Routes>
          <Route path="/" element={<Login />} />
          <Route path="/lost" element={<Private><Lost /></Private>} />
          <Route path="/found" element={<Private><Found /></Private>} />
        </Routes>
      </div>
    </>
  );
}
