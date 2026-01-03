import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api/client";
import { categories } from "../api/categories";
import { formatDate } from "../utils/dateUtils";
import "./found.css";


export default function FoundReports() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [searchTerm, setSearchTerm] = useState("");
  const [category, setCategory] = useState("");
  const [subcategory, setSubcategory] = useState("");
  const [subcatOptions, setSubcatOptions] = useState([]);
  const [statusFilter, setStatusFilter] = useState("UNCLAIMED"); // UNCLAIMED, MATCHED, RETURNED, ALL
  const [matches, setMatches] = useState({}); // { [foundId]: [lostItems] }
  const [showMatchesFor, setShowMatchesFor] = useState(null);
  const [confirming, setConfirming] = useState({}); // { [lostId_foundId]: true/false }
  const [confirmMsg, setConfirmMsg] = useState("");
  const navigate = useNavigate();

  const loadFoundItems = async () => {
    try {
      setLoading(true);
      setError("");
      const data = await api(`/items/found?status=${statusFilter}`);
      setItems(data);
    } catch (err) {
      // Check if it's a session expiry or authentication error
      if (err.status === 401 || err.status === 403 || err.message?.includes("Unauthorized") || err.message?.includes("authentication")) {
        // Session expired, redirect to login
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
      if (statusFilter) {
        params.append('status', statusFilter);
      }
      const url = `/items/found${params.toString() ? `?${params.toString()}` : ''}`;
      const data = await api(url);
      setItems(data);
    } catch (err) {
      setError(err.message || "Search failed. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const fetchMatches = async (foundId) => {
    setShowMatchesFor(foundId);
    setMatches(prev => ({ ...prev, [foundId]: null }));
    try {
      const lostMatches = await api(`/items/found/${foundId}/matches`);
      setMatches(prev => ({ ...prev, [foundId]: lostMatches }));
    } catch (err) {
      if (err.status === 401 || err.status === 403) {
        navigate("/", { replace: true });
        return;
      }
      setMatches(prev => ({ ...prev, [foundId]: [] }));
    }
  };

  const handleConfirmMatch = async (lostId, foundId) => {
    setConfirming(prev => ({ ...prev, [`${lostId}_${foundId}`]: true }));
    setConfirmMsg("");
    try {
      await api(`/items/lost/${lostId}/confirm-match/${foundId}`, { method: "POST" });
      setConfirmMsg("Match confirmed and handoff created successfully! Check the Handoff Queue to manage it.");
      alert("Match confirmed! A handoff has been automatically created. Go to Handoff Queue to schedule the return.");
      await loadFoundItems();
      await fetchMatches(foundId);
    } catch (err) {
      if (err.status === 401 || err.status === 403) {
        navigate("/", { replace: true });
        return;
      }
      setConfirmMsg(err.message || "Failed to confirm match.");
    } finally {
      setConfirming(prev => ({ ...prev, [`${lostId}_${foundId}`]: false }));
    }
  };

  useEffect(() => {
    loadFoundItems();
  }, [statusFilter]);

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
        <input className="input" type="text" placeholder="Search keywords..." title="Use comma separated keywords" value={searchTerm} onChange={handleSearchChange} />
        <select className="input" value={category} onChange={onCategoryChange}>
          <option value="">Select category</option>
          {categories.map(cat => <option key={cat.name} value={cat.name}>{cat.name}</option>)}
        </select>
        <select className="input" value={subcategory} onChange={onSubcategoryChange} disabled={!category}>
          <option value="">Select subcategory</option>
          {subcatOptions.map(sub => <option key={sub} value={sub}>{sub}</option>)}
        </select>
        <select className="input" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
          <option value="UNCLAIMED">Unclaimed</option>
          <option value="MATCHED">Matched</option>
          <option value="RETURNED">Returned</option>
          <option value="ALL">All</option>
        </select>
        <button className="btn" type="submit">Search</button>
        <button className="btn" type="button" onClick={() => {
            setSearchTerm("");
            setCategory("");
            setSubcategory("");
            setSubcatOptions([]);
            setStatusFilter("UNCLAIMED");
            loadFoundItems();
          }} style={{ background: "#6b7280" }}>Reset</button>
      </form>
      <div className="list">
        {items.length === 0 ? (
          <p>No found reports available.</p>
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
                  <div className="item-date">{formatDate(item.dateFound)}</div>
                  {item.description && (
                    <div className="item-description">{item.description}</div>
                  )}
                  <div className="item-reporter">
                    Reporter: {item.reporterName}
                    {item.reporterEmail && ` (${item.reporterEmail})`}
                  </div>
                  <span className="item-status">{item.status}</span>
                  {item.status !== 'RETURNED' && (
                    <div style={{marginTop: '12px'}}>
                      <button className="btn" onClick={() => fetchMatches(item.id)}>View Matches</button>
                    </div>
                  )}
                </div>
                {showMatchesFor === item.id && (
                  <div style={{padding: '16px', borderTop: '1px solid var(--border)', background: '#f8f9fa'}}>
                    <h4 style={{margin: '0 0 12px 0', fontSize: '16px'}}>Potential Matches (Lost Items):</h4>
                    {matches[item.id] == null ? (
                      <p>Loading matches...</p>
                    ) : matches[item.id].length === 0 ? (
                      <p>No matches found.</p>
                    ) : (
                      <div style={{display: 'flex', flexDirection: 'column', gap: '8px'}}>
                        {matches[item.id].map(lost => (
                          <div key={lost.id} style={{padding: '8px', background: 'white', borderRadius: '8px', border: '1px solid var(--border)'}}>
                            <div style={{fontWeight: 'bold', marginBottom: '4px'}}>{lost.title} ({lost.location})</div>
                            <div style={{fontSize: '14px', color: 'var(--muted)', marginBottom: '4px'}}>{lost.description}</div>
                            <div style={{fontSize: '13px', color: 'var(--muted)', marginBottom: '4px'}}>Date lost: {formatDate(lost.dateLost)}</div>
                            <div style={{fontSize: '13px', color: 'var(--muted)', marginBottom: '8px'}}>Owner: {lost.ownerName} {lost.ownerEmail && `(${lost.ownerEmail})`}</div>
                            <button 
                              className="btn" 
                              style={{fontSize: '12px', padding: '4px 8px'}} 
                              onClick={() => handleConfirmMatch(lost.id, item.id)} 
                              disabled={!!confirming[`${lost.id}_${item.id}`]}
                            >
                              {confirming[`${lost.id}_${item.id}`] ? 'Processing...' : 'Confirm Match & Create Handoff'}
                            </button>
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