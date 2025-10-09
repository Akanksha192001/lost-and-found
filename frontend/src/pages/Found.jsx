import React, { useState, useEffect } from "react";
import { api } from "../api/client";
import "./found.css";

const CATEGORY_OPTIONS = ["Electronics", "Books", "Clothing", "Accessories", "Other"];

export default function Found() {
  const [form, setForm] = useState({
    title: "",
    description: "",
    category: "Electronics",
    locationFound: "",
    dateFound: "",
    photoData: ""
  });
  const [photoPreview, setPhotoPreview] = useState("");
  const [items, setItems] = useState([]);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const onChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const onPhotoChange = (e) => {
    const file = e.target.files && e.target.files[0];
    if (!file) {
      setForm((prev) => ({ ...prev, photoData: "" }));
      setPhotoPreview("");
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      const result = typeof reader.result === "string" ? reader.result : "";
      setForm((prev) => ({ ...prev, photoData: result }));
      setPhotoPreview(result);
    };
    reader.readAsDataURL(file);
  };

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    setMessage("");
    try {
      await api("/items/found", {
        method: "POST",
        body: JSON.stringify(form)
      });
      setMessage("Found item listed. Verification will review duplicates automatically.");
      setForm({
        title: "",
        description: "",
        category: "Electronics",
        locationFound: "",
        dateFound: "",
        photoData: ""
      });
      setPhotoPreview("");
      load();
    } catch (err) {
      setError(err.message || "Could not submit found report.");
    }
  };

  const load = async () => {
    const data = await api("/items");
    setItems(data.filter(it => it.type === "FOUND"));
  };

  useEffect(() => { load(); }, []);

  return (
    <div className="card">
      <div className="header">
        <h2 className="pageTitle">Report Found Item</h2>
        <p className="subtitle">Attach a photo, categorize the item, and we&apos;ll scan for duplicates.</p>
      </div>
      <div className="grid">
        <form onSubmit={submit} className="form">
          <input className="input" name="title" placeholder="Item title" value={form.title} onChange={onChange} required />
          <textarea className="input" name="description" placeholder="Description" value={form.description} onChange={onChange} rows={3} />
          <label className="inputLabel" htmlFor="category">Category</label>
          <select className="input" id="category" name="category" value={form.category} onChange={onChange} required>
            {CATEGORY_OPTIONS.map(option => <option key={option} value={option}>{option}</option>)}
          </select>
          <input className="input" name="locationFound" placeholder="Location found" value={form.locationFound} onChange={onChange} required />
          <input className="input" name="dateFound" type="date" value={form.dateFound} onChange={onChange} required />
          <label className="inputLabel" htmlFor="photoInput">Attach photo</label>
          <input className="input" id="photoInput" name="photo" type="file" accept="image/*" onChange={onPhotoChange} />
          {photoPreview && (
            <img src={photoPreview} alt="Preview" className="preview" />
          )}
          <button className="btn" type="submit">Submit Found Report</button>
          {error && <p className="error">{error}</p>}
          {message && <p className="note">{message}</p>}
        </form>

        <div className="list">
          <h3>All Items</h3>
          <ul>
            {items.map(it => (
              <li key={it.id}>
                <div className="itemRow">
                  <div>
                    <strong>{it.title}</strong> <span className="badge">{it.category || it.type}</span>
                    <div>{it.location} - {it.dateISO || "Date TBD"}</div>
                    <div>Status: {it.status || "UNKNOWN"}</div>
                  </div>
                  {it.photoData && <img src={it.photoData} alt={it.title} className="thumbnail" />}
                </div>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
}
