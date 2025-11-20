import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api/client";
import { categories } from "../api/categories";
import { getRole } from "../auth";
import { formatDate } from "../utils/dateUtils";
import "./lost.css";


export default function LostReports() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const [category, setCategory] = useState("");
  const [subcategory, setSubcategory] = useState("");
  const [subcatOptions, setSubcatOptions] = useState([]);
  const navigate = useNavigate();

  const loadLostItems = async () => {
    try {
      setLoading(true);
      setError("");
      const userRole = getRole();
      // For USER role, load only their own reports; for ADMIN, load all reports
      const endpoint = userRole === "USER" ? "/items/lost/my" : "/items/lost";
      const data = await api(endpoint);
      setItems(data);
    } catch (err) {
      // Check if it's a session expiry or authentication error
      if (err.isAuthError || err.status === 401) {
        alert("Session expired. Please login again.");
        navigate("/", { replace: true });
        return;
      }
      if (err.isForbidden || err.status === 403) {
        setError("Access denied. You don't have permission to view this content.");
        return;
      }
      setError(err.message || "Could not load lost reports. Please try again.");
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
      const params = new URLSearchParams();
      
      if (searchTerm.trim()) {
        params.append('q', searchTerm.trim());
      }
      if (category) {
        params.append('category', category);
      }
      if (subcategory) {
        params.append('subcategory', subcategory);
      }

      const url = `/items/lost${params.toString() ? `?${params.toString()}` : ''}`;
      const data = await api(url);
      setItems(data);
    } catch (err) {
      if (err.isAuthError || err.status === 401) {
        alert("Session expired. Please login again.");
        navigate("/", { replace: true });
        return;
      }
      setError(err.message || "Search failed. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadLostItems();
  }, []);

  const userRole = getRole();
  const isUser = userRole === "USER";
  const pageTitle = isUser ? "My Reports" : "Lost Reports";
  const pageSubtitle = isUser ? "View and manage your lost item reports" : "Browse and search reported lost items";

  if (loading) {
    return (
      <div className="card">
        <div className="header">
          <h2 className="pageTitle">{pageTitle}</h2>
        </div>
        <p>Loading {isUser ? "your" : "lost"} reports...</p>
      </div>
    );
  }

  return (
    <div className="card">
      <div className="header">
        <h2 className="pageTitle">{pageTitle}</h2>
        <p className="subtitle">{pageSubtitle}</p>
      </div>
      
      {error && <p className="error">{error}</p>}
      
      {!isUser && (
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
        </form>
      )}

      <div className="list">
        {items.length === 0 ? (
          <p>{isUser ? "You haven't reported any lost items yet." : "No lost reports available."}</p>
        ) : (
          <div className="items-grid">
            {items.map(item => (
              <div key={item.id} className="item-card">
                {item.imageData ? (
                  <img 
                    src={item.imageData} 
                    alt={item.title} 
                    className="item-image"
                    onError={(e) => {
                      e.target.onerror = null; // Prevent infinite loop
                      e.target.style.display = 'none';
                      e.target.nextElementSibling.style.display = 'flex';
                    }}
                  />
                ) : null}
                <div 
                  className="image-placeholder"
                  style={{ display: item.imageData ? 'none' : 'flex' }}
                >
                  No Image Available
                </div>
                <div className="item-content">
                  <h3 className="item-title">{item.title}</h3>
                  <div className="item-location">{item.location}</div>
                  <div className="item-date">{formatDate(item.dateLost)}</div>
                  {item.description && (
                    <div className="item-description">{item.description}</div>
                  )}
                  <div className="item-reporter">
                    Owner: {item.ownerName}
                    {item.ownerEmail && ` (${item.ownerEmail})`}
                    {item.ownerAddress && ` - ${item.ownerAddress}`}
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