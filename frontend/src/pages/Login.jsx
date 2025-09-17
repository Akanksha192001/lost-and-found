import React, { useState } from "react";
import { api } from "../api/client";

export default function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [msg, setMsg] = useState("");

  const register = async (e) => {
    e.preventDefault();
    const data = await api("/auth/register", {
      method: "POST",
      body: JSON.stringify({ name, email, password })
    });
    setMsg(`Registered: ${data.name}`);
  };

  const login = async (e) => {
    e.preventDefault();
    const data = await api("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password })
    });
    setMsg(`Logged in as ${data.email}`);
    localStorage.setItem("uid", data.id);
  };

  return (
    <div>
      <h2>Login / Register</h2>
      <form onSubmit={login}>
        <input placeholder="Email" value={email} onChange={e=>setEmail(e.target.value)} />
        <input placeholder="Password" type="password" value={password} onChange={e=>setPassword(e.target.value)} />
        <button type="submit">Login</button>
      </form>

      <h3>Create Account</h3>
      <form onSubmit={register}>
        <input placeholder="Name" value={name} onChange={e=>setName(e.target.value)} />
        <input placeholder="Email" value={email} onChange={e=>setEmail(e.target.value)} />
        <input placeholder="Password" type="password" value={password} onChange={e=>setPassword(e.target.value)} />
        <button type="submit">Register</button>
      </form>
      <p>{msg}</p>
    </div>
  );
}
