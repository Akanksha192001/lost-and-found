import React, { useState } from "react";
import { api } from "../api/client";

export default function Lost() {
  const [form, setForm] = useState({ title:"", description:"", locationLastSeen:"", dateLost:"" });
  const onChange = e => setForm({ ...form, [e.target.name]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    const uid = localStorage.getItem("uid") || "demoUser";
    const data = await api("/items/lost", {
      method: "POST",
      headers: { "X-UID": uid },
      body: JSON.stringify(form)
    });
    alert("Lost item submitted: " + data.id);
  };

  return (
    <div>
      <h2>Report Lost Item</h2>
      <form onSubmit={submit}>
        <input name="title" placeholder="Item title" onChange={onChange} />
        <input name="description" placeholder="Description" onChange={onChange} />
        <input name="locationLastSeen" placeholder="Location last seen" onChange={onChange} />
        <input name="dateLost" type="date" onChange={onChange} />
        <button type="submit">Submit</button>
      </form>
    </div>
  );
}
