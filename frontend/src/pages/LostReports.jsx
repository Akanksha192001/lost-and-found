import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api/client";
import { categories } from "../api/categories";
import "./lost.css";


export default function LostReports() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const [matches, setMatches] = useState({}); // { [lostId]: [foundItems] }
  const [showMatchesFor, setShowMatchesFor] = useState(null);
  const [confirming, setConfirming] = useState({}); // { [lostId_foundId]: true/false }
  const [confirmMsg, setConfirmMsg] = useState("");
  const [category, setCategory] = useState("");
  const [subcategory, setSubcategory] = useState("");
  const [subcatOptions, setSubcatOptions] = useState([]);
  const navigate = useNavigate();

  const loadLostItems = async () => {
    try {
      setLoading(true);
      setError("");
      const data = await api("/items/lost");
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
      setError(err.message || "Could not load lost reports. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const fetchMatches = async (lostId) => {
    setShowMatchesFor(lostId);
    setMatches(prev => ({ ...prev, [lostId]: null }));
    try {
      const foundMatches = await api(`/items/lost/${lostId}/matches`);
      setMatches(prev => ({ ...prev, [lostId]: foundMatches }));
    } catch (err) {
      setMatches(prev => ({ ...prev, [lostId]: [] }));
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
      let url = searchTerm.trim() ? `/items/lost/search?q=${encodeURIComponent(searchTerm)}` : "/items/lost";
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

  const handleConfirmMatch = async (lostId, foundId) => {
    setConfirming(prev => ({ ...prev, [`${lostId}_${foundId}`]: true }));
    setConfirmMsg("");
    try {
      await api(`/items/lost/${lostId}/confirm-match/${foundId}`, { method: "POST" });
      setConfirmMsg("Match confirmed successfully.");
      // Optionally refresh lost items or matches
      await loadLostItems();
      await fetchMatches(lostId);
    } catch (err) {
      setConfirmMsg(err.message || "Failed to confirm match.");
    } finally {
      setConfirming(prev => ({ ...prev, [`${lostId}_${foundId}`]: false }));
    }
  };

  useEffect(() => {
    loadLostItems();
  }, []);

  if (loading) {
    return (
      <div className="card">
        <div className="header">
          <h2 className="pageTitle">Lost Reports</h2>
        </div>
        <p>Loading lost reports...</p>
      </div>
    );
  }

  return (
    <div className="card">
      <div className="header">
        <h2 className="pageTitle">Lost Reports</h2>
        <p className="subtitle">Browse and search reported lost items</p>
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
      </form>

      <div className="list">
        {items.length === 0 ? (
          <p>No lost reports available.</p>
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
                  <div className="item-date">{item.dateLost}</div>
                  {item.description && (
                    <div className="item-description">{item.description}</div>
                  )}
                  <div className="item-reporter">
                    Owner: {item.ownerName}
                    {item.ownerEmail && ` (${item.ownerEmail})`}
                    {item.ownerAddress && ` - ${item.ownerAddress}`}
                  </div>
                  <span className="item-status">{item.status}</span>
                  <div style={{marginTop: '12px'}}>
                    <button className="btn" onClick={() => fetchMatches(item.id)}>View Matches</button>
                  </div>
                </div>
                {showMatchesFor === item.id && (
                  <div style={{padding: '16px', borderTop: '1px solid var(--border)', background: '#f8f9fa'}}>
                    <h4 style={{margin: '0 0 12px 0', fontSize: '16px'}}>Potential Matches (Found Items):</h4>
                    {matches[item.id] == null ? (
                      <p>Loading matches...</p>
                    ) : matches[item.id].length === 0 ? (
                      <p>No matches found.</p>
                    ) : (
                      <div style={{display: 'flex', flexDirection: 'column', gap: '8px'}}>
                        {matches[item.id].map(found => (
                          <div key={found.id} style={{padding: '8px', background: 'white', borderRadius: '8px', border: '1px solid var(--border)'}}>
                            <div style={{fontWeight: 'bold', marginBottom: '4px'}}>{found.title} ({found.location})</div>
                            <div style={{fontSize: '14px', color: 'var(--muted)', marginBottom: '4px'}}>{found.description}</div>
                            <div style={{fontSize: '13px', color: 'var(--muted)', marginBottom: '4px'}}>Date found: {found.dateFound}</div>
                            <div style={{fontSize: '13px', color: 'var(--muted)', marginBottom: '8px'}}>Reporter: {found.reporterName} {found.reporterEmail && `(${found.reporterEmail})`}</div>
                            <button className="btn" style={{fontSize: '12px', padding: '4px 8px'}} onClick={() => handleConfirmMatch(item.id, found.id)} disabled={!!confirming[`${item.id}_${found.id}`]}>Confirm Match</button>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}
                {showMatchesFor === item.id && confirmMsg && (
                  <div style={{padding: '12px', background: '#d4edda', color: '#155724', borderTop: '1px solid var(--border)'}}>{confirmMsg}</div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}