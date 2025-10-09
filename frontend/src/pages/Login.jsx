import React, { useState } from "react";
import { api } from "../api/client";
import { useNavigate } from "react-router-dom";
import { setSession, landingRouteFor } from "../auth";
import "./login.css";

export default function Login() {
  const [mode, setMode] = useState("login"); // 'login' or 'register'
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [msg, setMsg] = useState("");
  const [role, setRole] = useState("STUDENT");
  const nav = useNavigate();

  const onRegister = async (e) => {
    e.preventDefault();
    const data = await api("/auth/register", {
      method: "POST",
      body: JSON.stringify({ name, email, password, role })
    });
    setMsg(`Account created for ${data.name}. You can sign in now.`);
    setMode("login");
    setRole("STUDENT");
  };

  const onLogin = async (e) => {
    e.preventDefault();
    const data = await api("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password })
    });
    setSession({ id: data.id, role: data.role, token: data.id });
    nav(landingRouteFor(data.role));
  };

  return (
    <div className="card auth-grid">
      <div className="illustration">
        <img src="https://images.unsplash.com/photo-1531297484001-80022131f5a1?q=80&w=1200&auto=format&fit=crop" alt="" className="hero-img" />
      </div>
      <div className="formWrap">
        <h1 className="title">{mode === "login" ? "Welcome Back" : "Welcome to NEIU Lost & Found"}</h1>
        <p className="subtitle">{mode === "login" ? "Sign in to continue" : "Create your account"}</p>

        {mode === "login" ? (
          <form onSubmit={onLogin} className="stack">
            <input className="input" placeholder="Email" value={email} onChange={e=>setEmail(e.target.value)} />
            <input className="input" placeholder="Password" type="password" value={password} onChange={e=>setPassword(e.target.value)} />
            <button className="btn" type="submit">Sign In</button>
            <div className="switch">New here? <button type="button" onClick={()=>setMode('register')}>Create an account</button></div>
          </form>
        ) : (
          <form onSubmit={onRegister} className="stack">
            <input className="input" placeholder="Full name" value={name} onChange={e=>setName(e.target.value)} />
            <input className="input" placeholder="Email" value={email} onChange={e=>setEmail(e.target.value)} />
            <input className="input" placeholder="Password (8+ chars)" type="password" value={password} onChange={e=>setPassword(e.target.value)} />
            <label className="inputLabel" htmlFor="role">Role</label>
            <select className="input" id="role" value={role} onChange={e=>setRole(e.target.value)}>
              <option value="STUDENT">Student</option>
              <option value="ADMIN">Admin</option>
              <option value="SECURITY">Security</option>
            </select>
            <button className="btn" type="submit">Create Account</button>
            <div className="note">Use a strong password. You can change it later when we add profile settings.</div>
            <div className="switch">Already have an account? <button type="button" onClick={()=>setMode('login')}>Sign in</button></div>
          </form>
        )}
        {msg && <p className="note">{msg}</p>}
      </div>
    </div>
  );
}
