import React, { useState, useEffect } from "react";
import { api } from "../api/client";
import "./found.css";

export default function Found() {
  const [form, setForm] = useState({ title:"", description:"", locationFound:"", dateFound:"" });
  const [items, setItems] = useState([]);

  const onChange = e => setForm({ ...form, [e.target.name]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    const uid = localStorage.getItem("uid") || "demoUser";
    await api("/items/found", {
      method: "POST",
      headers: { "X-UID": uid },
      body: JSON.stringify(form)
    });
    setForm({ title:"", description:"", locationFound:"", dateFound:"" });
    load();
  };

  const load = async () => setItems(await api("/items"));

  useEffect(() => { load(); }, []);

  return (
    <div className="card">
      <div className="header">
        <h2 className="pageTitle">Report Found Item</h2>
      </div>
      <div className="grid">
        <form onSubmit={submit} className="form">
          <input className="input" name="title" placeholder="Item title" value={form.title} onChange={onChange} />
          <input className="input" name="description" placeholder="Description" value={form.description} onChange={onChange} />
          <input className="input" name="locationFound" placeholder="Location found" value={form.locationFound} onChange={onChange} />
          <input className="input" name="dateFound" type="date" value={form.dateFound} onChange={onChange} />
          <button className="btn" type="submit">Submit Found Report</button>
        </form>

        <div className="list">
          <h3>All Items</h3>
          <ul>
            {items.map(it => (
              <li key={it.id}>
                [{it.type}] {it.title} - {it.location} ({it.dateISO})
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
}
