import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api/client";
import { categories } from "../api/categories";
import { getUserName, getUserEmail, getRole } from "../auth";
import { getTodayDate } from "../utils/dateUtils";
import "./lost.css";

export default function Lost() {

  const userRole = getRole();
  const [form, setForm] = useState({ 
    title: "", 
    description: "", 
    location: "", 
    dateLost: getTodayDate(), 
    imageData: "",
    ownerName: userRole === "ADMIN" ? "" : getUserName(),
    ownerEmail: userRole === "ADMIN" ? "" : getUserEmail(), 
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
      setForm((prev) => ({ ...prev, imageData: "" }));
      setPhotoPreview("");
      return;
    }
    
    // Check file size (limit to 5MB)
    const maxSize = 5 * 1024 * 1024; // 5MB in bytes
    if (file.size > maxSize) {
      setError("Image file size must be less than 5MB. Please choose a smaller image.");
      e.target.value = ""; // Clear the file input
      return;
    }
    
    // Check file type
    if (!file.type.startsWith('image/')) {
      setError("Please select a valid image file.");
      e.target.value = "";
      return;
    }
    
    const reader = new FileReader();
    reader.onload = () => {
      const result = typeof reader.result === "string" ? reader.result : "";
      
      // Optionally compress image if it's large
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        let width = img.width;
        let height = img.height;
        
        // Resize if image is too large (max 1200px on longest side)
        const maxDimension = 1200;
        if (width > maxDimension || height > maxDimension) {
          if (width > height) {
            height = (height / width) * maxDimension;
            width = maxDimension;
          } else {
            width = (width / height) * maxDimension;
            height = maxDimension;
          }
        }
        
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0, width, height);
        
        // Convert to base64 with compression (0.85 quality for JPEG)
        const compressedDataUrl = canvas.toDataURL('image/jpeg', 0.85);
        setForm((prev) => ({ ...prev, imageData: compressedDataUrl }));
        setPhotoPreview(compressedDataUrl);
      };
      img.onerror = () => {
        setError("Failed to load image. Please try another file.");
      };
      img.src = result;
    };
    reader.onerror = () => {
      setError("Failed to read image file. Please try again.");
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
      const data = await api("/items/lost", {
        method: "POST",
        body: JSON.stringify(form)
      });
      setMessage("Lost item submitted successfully: " + data.id);
      setForm({ 
        title: "", 
        description: "", 
        location: "", 
        dateLost: getTodayDate(), 
        imageData: "",
        ownerName: userRole === "ADMIN" ? "" : getUserName(),
        ownerEmail: userRole === "ADMIN" ? "" : getUserEmail(), 
        category: "", 
        subcategory: "" 
      });
      setPhotoPreview("");
    } catch (err) {
      // Check if it's a session expiry or authentication error
      if (err.isAuthError || err.status === 401) {
        // Show session expired message and redirect to login
        alert("Session expired. Please login again.");
        navigate("/", { replace: true });
        return;
      }
      if (err.isForbidden || err.status === 403) {
        // Show access denied message
        setError("Access denied. You don't have permission to perform this action.");
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
        {/* 1. Essential Item Details */}
        <div className="form-section">
          <h3 className="section-title">Item Details</h3>
        </div>
        
        <div className="field-group">
          <label className="inputLabel" htmlFor="title">Item Title *</label>
          <input className="input" id="title" name="title" placeholder="What item did you lose?" value={form.title} onChange={onChange} required />
        </div>
        
        <div className="field-group">
          <label className="inputLabel" htmlFor="description">Description</label>
          <textarea className="input" id="description" name="description" placeholder="Describe the item (color, size, brand, etc.)" value={form.description} onChange={onChange} rows={3} />
        </div>
        
        {/* 2. Categorization */}
        <div className="form-section">
          <h3 className="section-title">Category</h3>
        </div>
        
        <div className="field-group">
          <label className="inputLabel" htmlFor="category">Category *</label>
          <select className="input" id="category" name="category" value={form.category} onChange={onCategoryChange} required>
            <option value="">Select item category</option>
            {categories.map(cat => <option key={cat.name} value={cat.name}>{cat.name}</option>)}
          </select>
        </div>
        
        <div className="field-group">
          <label className="inputLabel" htmlFor="subcategory">Subcategory *</label>
          <select className="input" id="subcategory" name="subcategory" value={form.subcategory} onChange={onSubcategoryChange} required disabled={!form.category}>
            <option value="">Select subcategory</option>
            {subcatOptions.map(sub => <option key={sub} value={sub}>{sub}</option>)}
          </select>
        </div>
        
        {/* 3. Location & Date */}
        <div className="form-section">
          <h3 className="section-title">When & Where</h3>
        </div>
        
        <div className="field-group">
          <label className="inputLabel" htmlFor="location">Location Last Seen *</label>
          <input className="input" id="location" name="location" placeholder="Where did you last see this item?" value={form.location} onChange={onChange} required />
        </div>
        
        <div className="field-group">
          <label className="inputLabel" htmlFor="dateLost">Date Lost *</label>
          <input className="input" id="dateLost" name="dateLost" type="date" value={form.dateLost} onChange={onChange} required />
        </div>
        
        {/* 4. Photo Upload */}
        <div className="form-section">
          <h3 className="section-title">Photo</h3>
        </div>
        
        <div className="field-group">
          <label className="inputLabel" htmlFor="photoInput">Attach Photo</label>
          <input className="input" id="photoInput" name="photo" type="file" accept="image/*" onChange={onPhotoChange} />
          {photoPreview && (
            <img src={photoPreview} alt="Preview" className="preview" />
          )}
        </div>
        
        {/* 5. Contact Information (ADMIN only) */}
        {userRole === "ADMIN" && (
          <>
            <div className="form-section">
              <h3 className="section-title">Owner Contact</h3>
            </div>
            
            <div className="field-group">
              <label className="inputLabel" htmlFor="ownerName">Owner Name</label>
              <input className="input" id="ownerName" name="ownerName" placeholder="Name of the person who lost the item" value={form.ownerName} onChange={onChange} />
            </div>
            
            <div className="field-group">
              <label className="inputLabel" htmlFor="ownerEmail">Owner Email</label>
              <input className="input" id="ownerEmail" name="ownerEmail" type="email" placeholder="Email of the person who lost the item" value={form.ownerEmail} onChange={onChange} />
            </div>
          </>
        )}
        
        <button className="btn" type="submit">Submit Lost Report</button>
        {error && <p className="error">{error}</p>}
        {message && <p className="note">{message}</p>}
      </form>
    </div>
  );
}
