import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api/client";
import { categories } from "../api/categories";
import "./found.css";

export default function Found() {
  const [form, setForm] = useState({
    title: "",
    description: "",
    location: "",
    dateFound: "",
    imageUrl: "",
    reporterName: "",
    reporterEmail: "",
    reporterAddress: "",
    category: "",
    subcategory: ""
  });
  const [photoPreview, setPhotoPreview] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("");
  const [subcatOptions, setSubcatOptions] = useState([]);
  const navigate = useNavigate();

  const onChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const onPhotoChange = (e) => {
    const file = e.target.files && e.target.files[0];
    if (!file) {
      setForm((prev) => ({ ...prev, imageUrl: "" }));
      setPhotoPreview("");
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      const result = typeof reader.result === "string" ? reader.result : "";
      setForm((prev) => ({ ...prev, imageUrl: result }));
      setPhotoPreview(result);
    };
    reader.readAsDataURL(file);
  };

  const onCategoryChange = (e) => {
    const cat = e.target.value;
    setSelectedCategory(cat);
    setForm({ ...form, category: cat, subcategory: "" });
    const found = categories.find(c => c.name === cat);
    setSubcatOptions(found ? found.subcategories : []);
  };

  const onSubcategoryChange = (e) => {
    setForm({ ...form, subcategory: e.target.value });
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
        location: "",
        dateFound: "",
        imageUrl: "",
        reporterName: "",
        reporterEmail: "",
        reporterAddress: "",
        category: "",
        subcategory: ""
      });
      setPhotoPreview("");
    } catch (err) {
      if (err.status === 401 || err.status === 403 || err.message?.includes("Unauthorized") || err.message?.includes("authentication")) {
        localStorage.removeItem("token");
        sessionStorage.removeItem("token");
        navigate("/", { replace: true });
        return;
      }
      setError(err.message || "Could not submit found report. Please try again.");
    }
  };

  return (
    <div className="card">
      <div className="header">
        <h2 className="pageTitle">Report Found Item</h2>
        <p className="subtitle">Attach a photo and we'll scan for duplicates.</p>
      </div>
      <form onSubmit={submit} className="form">
        <input className="input" name="title" placeholder="Item title" value={form.title} onChange={onChange} required />
        <textarea className="input" name="description" placeholder="Description" value={form.description} onChange={onChange} rows={3} />
        <input className="input" name="location" placeholder="Location found" value={form.location} onChange={onChange} required />
        <input className="input" name="dateFound" type="date" value={form.dateFound} onChange={onChange} required />
        <label className="inputLabel" htmlFor="photoInput">Attach photo</label>
        <input className="input" id="photoInput" name="photo" type="file" accept="image/*" onChange={onPhotoChange} />
        {photoPreview && (
          <img src={photoPreview} alt="Preview" className="preview" />
        )}
        <input className="input" name="reporterName" placeholder="Reporter name" value={form.reporterName} onChange={onChange} />
        <input className="input" name="reporterEmail" placeholder="Reporter email" value={form.reporterEmail} onChange={onChange} />
        <input className="input" name="reporterAddress" placeholder="Reporter address" value={form.reporterAddress} onChange={onChange} />
        <select className="input" name="category" value={form.category} onChange={onCategoryChange} required>
          <option value="">Select category</option>
          {categories.map(cat => <option key={cat.name} value={cat.name}>{cat.name}</option>)}
        </select>
        <select className="input" name="subcategory" value={form.subcategory} onChange={onSubcategoryChange} required disabled={!form.category}>
          <option value="">Select subcategory</option>
          {subcatOptions.map(sub => <option key={sub} value={sub}>{sub}</option>)}
        </select>
        <button className="btn" type="submit">Submit Found Report</button>
        {error && <p className="error">{error}</p>}
        {message && <p className="note">{message}</p>}
      </form>
    </div>
  );
}
