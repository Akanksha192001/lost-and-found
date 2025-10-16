import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api/client";
import { categories } from "../api/categories";
import "./lost.css";

export default function Lost() {
  const [form, setForm] = useState({ title: "", description: "", location: "", dateLost: "", imageUrl: "", ownerName: "", ownerEmail: "", ownerAddress: "", category: "", subcategory: "" });
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("");
  const [subcatOptions, setSubcatOptions] = useState([]);
  const navigate = useNavigate();
  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

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
      const data = await api("/items/lost", {
        method: "POST",
        body: JSON.stringify(form)
      });
      setMessage("Lost item submitted successfully: " + data.id);
      setForm({ title: "", description: "", location: "", dateLost: "", imageUrl: "", ownerName: "", ownerEmail: "", ownerAddress: "", category: "", subcategory: "" });
    } catch (err) {
      // Check if it's a session expiry or authentication error
      if (err.status === 401 || err.status === 403 || err.message?.includes("Unauthorized") || err.message?.includes("authentication")) {
        // Clear any stored auth tokens and redirect to login
        localStorage.removeItem("token");
        sessionStorage.removeItem("token");
        navigate("/", { replace: true });
        return;
      }
      setError(err.message || "Unable to submit lost report. Please try again.");
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
        <input className="input" name="location" placeholder="Location last seen" value={form.location} onChange={onChange} required />
        <input className="input" name="dateLost" type="date" value={form.dateLost} onChange={onChange} required />
        <input className="input" name="imageUrl" placeholder="Image URL (optional)" value={form.imageUrl} onChange={onChange} />
        <input className="input" name="ownerName" placeholder="Owner name" value={form.ownerName} onChange={onChange} />
        <input className="input" name="ownerEmail" placeholder="Owner email" value={form.ownerEmail} onChange={onChange} />
        <input className="input" name="ownerAddress" placeholder="Owner address" value={form.ownerAddress} onChange={onChange} />
        <select className="input" name="category" value={form.category} onChange={onCategoryChange} required>
          <option value="">Select category</option>
          {categories.map(cat => <option key={cat.name} value={cat.name}>{cat.name}</option>)}
        </select>
        <select className="input" name="subcategory" value={form.subcategory} onChange={onSubcategoryChange} required disabled={!form.category}>
          <option value="">Select subcategory</option>
          {subcatOptions.map(sub => <option key={sub} value={sub}>{sub}</option>)}
        </select>
        <button className="btn" type="submit">Submit Lost Report</button>
        {error && <p className="error">{error}</p>}
        {message && <p className="note">{message}</p>}
      </form>
    </div>
  );
}
