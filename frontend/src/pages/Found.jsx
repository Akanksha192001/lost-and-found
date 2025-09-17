import React, { useState, useEffect } from "react";
import { api } from "../api/client";

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
    load();
  };

  const load = async () => setItems(await api("/items"));

  useEffect(() => { load(); }, []);

  return (
    <div>
      <h2>Report Found Item</h2>
      <form onSubmit={submit}>
        <input name="title" placeholder="Item title" onChange={onChange} />
        <input name="description" placeholder="Description" onChange={onChange} />
        <input name="locationFound" placeholder="Location found" onChange={onChange} />
        <input name="dateFound" type="date" onChange={onChange} />
        <button type="submit">Submit</button>
      </form>

      <h3>All Items</h3>
      <ul>
        {items.map(it => (
          <li key={it.id}>
            [{it.type}] {it.title} — {it.location} ({it.dateISO})
          </li>
        ))}
      </ul>
    </div>
  );
}
