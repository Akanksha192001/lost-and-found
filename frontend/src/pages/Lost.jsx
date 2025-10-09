import React, { useState } from "react";
import { api } from "../api/client";
import "./lost.css";

export default function Lost() {
  const [form, setForm] = useState({ title: "", description: "", locationLastSeen: "", dateLost: "" });
  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    try {
      const data = await api("/items/lost", {
        method: "POST",
        body: JSON.stringify(form)
      });
      alert("Lost item submitted: " + data.id);
      setForm({ title: "", description: "", locationLastSeen: "", dateLost: "" });
    } catch (err) {
      alert(err.message || "Unable to submit lost report.");
    }
  };

  return (
    <div className="card">
      <div className="header">
        <h2 className="pageTitle">Report Lost Item</h2>
      </div>
      <form onSubmit={submit} className="form">
        <input className="input" name="title" placeholder="Item title" value={form.title} onChange={onChange} required />
        <input className="input" name="description" placeholder="Description" value={form.description} onChange={onChange} />
        <input className="input" name="locationLastSeen" placeholder="Location last seen" value={form.locationLastSeen} onChange={onChange} required />
        <input className="input" name="dateLost" type="date" value={form.dateLost} onChange={onChange} required />
        <button className="btn" type="submit">Submit Lost Report</button>
      </form>
    </div>
  );
}
