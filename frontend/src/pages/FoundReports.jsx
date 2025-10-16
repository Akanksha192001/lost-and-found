import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api/client";
import { categories } from "../api/categories";
import "./found.css";


export default function FoundReports() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const [category, setCategory] = useState("");
  const [subcategory, setSubcategory] = useState("");
  const [subcatOptions, setSubcatOptions] = useState([]);
  const navigate = useNavigate();

  const loadFoundItems = async () => {
    try {
      setLoading(true);
      setError("");
      const data = await api("/items/found");
      setItems(data);
    } catch (err) {
      // Check if it's a session expiry or authentication error
      if (err.status === 401 || err.status === 403 || err.message?.includes("Unauthorized") || err.message?.includes("authentication")) {
        // Clear any stored auth tokens and redirect to login
        localStorage.removeItem("token");
        sessionStorage.removeItem("token");
        navigate("/", { replace: true });
        return;
      }
      setError(err.message || "Could not load found reports. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
  };

  const onCategoryChange = (e) => {
    const cat = e.target.value;
    setCategory(cat);
    setSubcategory("");
    const found = categories.find(c => c.name === cat);
    setSubcatOptions(found ? found.subcategories : []);
  };

  const onSubcategoryChange = (e) => {
    setSubcategory(e.target.value);
  };

  const handleSearchSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      let url = searchTerm.trim() ? `/items/found/search?q=${encodeURIComponent(searchTerm)}` : "/items/found";
      if (category) url += `&category=${encodeURIComponent(category)}`;
      if (subcategory) url += `&subcategory=${encodeURIComponent(subcategory)}`;
      const data = await api(url);
      setItems(data);
    } catch (err) {
      setError(err.message || "Search failed. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadFoundItems();
  }, []);

  if (loading) {
    return (
      <div className="card">
        <div className="header">
          <h2 className="pageTitle">Found Reports</h2>
        </div>
        <p>Loading found reports...</p>
      </div>
    );
  }

  return (
    <div className="card">
      <div className="header">
        <h2 className="pageTitle">Found Reports</h2>
        <p className="subtitle">Browse and search reported found items</p>
      </div>
      {error && <p className="error">{error}</p>}
      <form onSubmit={handleSearchSubmit} className="searchForm">
        <input className="input" type="text" placeholder="Search keywords (comma separated)" value={searchTerm} onChange={handleSearchChange} />
        <select className="input" value={category} onChange={onCategoryChange}>
          <option value="">Select category</option>
          {categories.map(cat => <option key={cat.name} value={cat.name}>{cat.name}</option>)}
        </select>
        <select className="input" value={subcategory} onChange={onSubcategoryChange} disabled={!category}>
          <option value="">Select subcategory</option>
          {subcatOptions.map(sub => <option key={sub} value={sub}>{sub}</option>)}
        </select>
        <button className="btn" type="submit">Search</button>
        <button className="btn" type="button" onClick={loadFoundItems}>Reset</button>
      </form>
      <div className="list">
        {items.length === 0 ? (
          <p>No found reports available.</p>
        ) : (
          <div className="items-grid">
            {items.map(item => (
              <div key={item.id} className="item-card">
                {item.imageUrl ? (
                  <img src={item.imageUrl} alt={item.title} className="item-image" />
                ) : (
                  <div className="image-placeholder">
                    No Image Available
                  </div>
                )}
                <div className="item-content">
                  <h3 className="item-title">{item.title}</h3>
                  <div className="item-location">{item.location}</div>
                  <div className="item-date">{item.dateFound}</div>
                  {item.description && (
                    <div className="item-description">{item.description}</div>
                  )}
                  <div className="item-reporter">
                    Reporter: {item.reporterName}
                    {item.reporterEmail && ` (${item.reporterEmail})`}
                    {item.reporterAddress && ` - ${item.reporterAddress}`}
                  </div>
                  <span className="item-status">{item.status}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}