import React from "react";
import { Routes, Route } from "react-router-dom";
import NavBar from "./components/NavBar";
import Login from "./pages/Login";
import Lost from "./pages/Lost";
import Found from "./pages/Found";

export default function App() {
  return (
    <>
      <NavBar />
      <div style={{ padding: 16 }}>
        <Routes>
          <Route path="/" element={<Login />} />
          <Route path="/lost" element={<Lost />} />
          <Route path="/found" element={<Found />} />
        </Routes>
      </div>
    </>
  );
}
