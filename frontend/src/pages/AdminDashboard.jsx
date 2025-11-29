import React, { useState, useEffect } from 'react';
import { api } from '../api/client';
import './Admin.css';

const AdminDashboard = () => {
  const [activeTab, setActiveTab] = useState('matching'); // 'matching' or 'handoffs'
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  
  // Matching state
  const [matchData, setMatchData] = useState([]);
  const [matchLoading, setMatchLoading] = useState(true);
  const [matchFilter, setMatchFilter] = useState('ALL'); // ALL, MATCHED, UNMATCHED
  const [expandedItems, setExpandedItems] = useState(new Set());
  
  // AI Analysis state
  const [aiAnalysis, setAiAnalysis] = useState({});
  const [aiLoading, setAiLoading] = useState({});
  const [aiError, setAiError] = useState({});
  const [showAiAnalysis, setShowAiAnalysis] = useState({});
  
  // Handoff state
  const [handoffs, setHandoffs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState('ALL');
  const [selectedHandoff, setSelectedHandoff] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [showScheduleModal, setShowScheduleModal] = useState(false);
  const [scheduleAction, setScheduleAction] = useState(''); // 'schedule' or 'reschedule'
  const [openOptionsMenu, setOpenOptionsMenu] = useState(null);
  const [menuPosition, setMenuPosition] = useState({ top: 0, left: 0 });

  // Form state for updating handoff
  const [updateForm, setUpdateForm] = useState({
    status: '',
    assignedTo: '',
    scheduledHandoffTime: '',
    handoffLocation: '',
    notes: '',
    cancellationReason: ''
  });

  // Form state for scheduling
  const [scheduleForm, setScheduleForm] = useState({
    scheduledHandoffTime: '',
    handoffLocation: '',
    assignedTo: ''
  });

  useEffect(() => {
    if (activeTab === 'matching') {
      fetchAllMatches();
    } else {
      fetchHandoffs();
    }
  }, [activeTab, filter, matchFilter]);

  // Close options menu when clicking outside
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (openOptionsMenu && !event.target.closest('.btn-options') && !event.target.closest('.options-dropdown-fixed')) {
        setOpenOptionsMenu(null);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [openOptionsMenu]);

  const fetchAllMatches = async () => {
    setMatchLoading(true);
    setError('');
    try {
      const response = await api('/items/matches/all');
      setMatchData(response);
    } catch (err) {
      setError('Failed to fetch matches');
      console.error('Error fetching matches:', err);
    } finally {
      setMatchLoading(false);
    }
  };

  const fetchHandoffs = async () => {
    setLoading(true);
    setError('');
    try {
      const params = filter !== 'ALL' ? `?status=${filter}` : '';
      const response = await api(`/handoffs${params}`);
      setHandoffs(response);
    } catch (err) {
      setError('Failed to fetch handoffs');
      console.error('Error fetching handoffs:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleViewDetails = (handoff) => {
    setSelectedHandoff(handoff);
    setUpdateForm({
      status: handoff.status,
      assignedTo: handoff.assignedTo || '',
      scheduledHandoffTime: handoff.scheduledHandoffTime 
        ? new Date(handoff.scheduledHandoffTime).toISOString().slice(0, 16) 
        : '',
      handoffLocation: handoff.handoffLocation || '',
      notes: handoff.notes || '',
      cancellationReason: handoff.cancellationReason || ''
    });
    setShowModal(true);
  };

  const handleUpdateHandoff = async (e) => {
    e.preventDefault();
    try {
      const updateData = {
        ...updateForm,
        scheduledHandoffTime: updateForm.scheduledHandoffTime 
          ? new Date(updateForm.scheduledHandoffTime).toISOString() 
          : null
      };

      await api(`/handoffs/${selectedHandoff.id}`, {
        method: 'PUT',
        body: JSON.stringify(updateData)
      });
      setShowModal(false);
      fetchHandoffs();
      alert('Handoff updated successfully!');
    } catch (err) {
      console.error('Error updating handoff:', err);
      alert('Failed to update handoff');
    }
  };

  const handleDeleteHandoff = async (id) => {
    if (!window.confirm('Are you sure you want to delete this handoff?')) {
      return;
    }
    try {
      await api(`/handoffs/${id}`, { method: 'DELETE' });
      await fetchHandoffs(); // Refresh handoff queue
      if (activeTab === 'matching') {
        await fetchAllMatches(); // Refresh matching tab if active
      }
      alert('Handoff deleted successfully! Match status has been reset.');
    } catch (err) {
      console.error('Error deleting handoff:', err);
      alert('Failed to delete handoff');
    }
  };

  const handleScheduleClick = (handoff, action) => {
    setSelectedHandoff(handoff);
    setScheduleAction(action);
    setScheduleForm({
      scheduledHandoffTime: handoff.scheduledHandoffTime 
        ? new Date(handoff.scheduledHandoffTime).toISOString().slice(0, 16)
        : '',
      handoffLocation: handoff.handoffLocation || '',
      assignedTo: handoff.assignedTo || ''
    });
    setShowScheduleModal(true);
    setOpenOptionsMenu(null);
  };

  const handleScheduleSubmit = async (e) => {
    e.preventDefault();
    try {
      const updateData = {
        status: 'SCHEDULED',
        scheduledHandoffTime: scheduleForm.scheduledHandoffTime 
          ? new Date(scheduleForm.scheduledHandoffTime).toISOString()
          : null,
        handoffLocation: scheduleForm.handoffLocation,
        assignedTo: scheduleForm.assignedTo,
        notes: selectedHandoff.notes || '',
        cancellationReason: ''
      };

      await api(`/handoffs/${selectedHandoff.id}`, {
        method: 'PUT',
        body: JSON.stringify(updateData)
      });
      setShowScheduleModal(false);
      fetchHandoffs();
      alert(`Handoff ${scheduleAction === 'schedule' ? 'scheduled' : 'rescheduled'} successfully!`);
    } catch (err) {
      console.error('Error scheduling handoff:', err);
      alert('Failed to schedule handoff');
    }
  };

  const handleCancelHandoff = async (handoff) => {
    if (!window.confirm('Are you sure you want to cancel this handoff and return it to PENDING status?')) {
      return;
    }
    
    try {
      const updateData = {
        status: 'PENDING',
        assignedTo: '',
        scheduledHandoffTime: null,
        handoffLocation: '',
        notes: handoff.notes || '',
        cancellationReason: ''
      };

      await api(`/handoffs/${handoff.id}`, {
        method: 'PUT',
        body: JSON.stringify(updateData)
      });
      setOpenOptionsMenu(null);
      fetchHandoffs();
      alert('Handoff cancelled and returned to PENDING status!');
    } catch (err) {
      console.error('Error cancelling handoff:', err);
      alert('Failed to cancel handoff');
    }
  };

  const handleRejectHandoff = async (handoffId) => {
    if (!window.confirm('Are you sure this is not a match? This will delete the handoff and reset the match status.')) {
      return;
    }
    try {
      await api(`/handoffs/${handoffId}`, { method: 'DELETE' });
      setOpenOptionsMenu(null);
      await fetchHandoffs();
      if (activeTab === 'matching') {
        await fetchAllMatches();
      }
      alert('Handoff rejected and removed. Match status has been reset.');
    } catch (err) {
      console.error('Error rejecting match:', err);
      alert('Failed to reject match');
    }
  };

  const handleCompleteHandoff = async (handoff) => {
    if (!window.confirm('Confirm that the item has been successfully handed off to the owner?')) {
      return;
    }
    try {
      const updateData = {
        status: 'COMPLETED',
        assignedTo: handoff.assignedTo || '',
        scheduledHandoffTime: handoff.scheduledHandoffTime 
          ? new Date(handoff.scheduledHandoffTime).toISOString()
          : null,
        handoffLocation: handoff.handoffLocation || '',
        notes: handoff.notes || '',
        cancellationReason: ''
      };

      await api(`/handoffs/${handoff.id}`, {
        method: 'PUT',
        body: JSON.stringify(updateData)
      });
      setOpenOptionsMenu(null);
      await fetchHandoffs();
      if (activeTab === 'matching') {
        await fetchAllMatches();
      }
      alert('Handoff completed successfully! Items have been moved to returned items archive.');
    } catch (err) {
      console.error('Error completing handoff:', err);
      alert('Failed to complete handoff');
    }
  };

  const toggleOptionsMenu = (handoffId, event) => {
    if (openOptionsMenu === handoffId) {
      setOpenOptionsMenu(null);
    } else {
      const buttonRect = event.currentTarget.getBoundingClientRect();
      const menuWidth = 200;
      const estimatedMenuHeight = 200;
      
      // Horizontal positioning - align right edge of menu with right edge of button
      let leftPosition = buttonRect.right - menuWidth;
      
      // If menu would go off left edge, align with button left instead
      if (leftPosition < 10) {
        leftPosition = buttonRect.left;
      }
      
      // Vertical positioning - check if button is near bottom of window
      const spaceBelow = window.innerHeight - buttonRect.bottom;
      let topPosition;
      
      if (spaceBelow < estimatedMenuHeight) {
        // Show menu ending just above button if near bottom
        topPosition = buttonRect.top;
        // Transform to move menu up by its own height using CSS
        setMenuPosition({
          top: topPosition,
          left: leftPosition,
          transform: 'translateY(-100%)'
        });
        setOpenOptionsMenu(handoffId);
        return;
      } else {
        // Show below button
        topPosition = buttonRect.bottom + 5;
      }
      
      setMenuPosition({
        top: topPosition,
        left: leftPosition
      });
      setOpenOptionsMenu(handoffId);
    }
  };

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'PENDING': return 'badge-pending';
      case 'IN_PROGRESS': return 'badge-in-progress';
      case 'SCHEDULED': return 'badge-scheduled';
      case 'COMPLETED': return 'badge-completed';
      case 'CANCELLED': return 'badge-cancelled';
      default: return '';
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'Not set';
    return new Date(dateString).toLocaleString();
  };

  const toggleExpandItem = (itemId) => {
    const newExpanded = new Set(expandedItems);
    if (newExpanded.has(itemId)) {
      newExpanded.delete(itemId);
    } else {
      newExpanded.add(itemId);
    }
    setExpandedItems(newExpanded);
  };

  const getConfidenceBadgeClass = (score) => {
    if (score >= 70) return 'confidence-high';
    if (score >= 40) return 'confidence-medium';
    return 'confidence-low';
  };

  const getConfidenceLabel = (score) => {
    if (score >= 70) return 'High';
    if (score >= 40) return 'Medium';
    return 'Low';
  };

  const handleConfirmMatch = async (foundItemId, lostItemId) => {
    if (!window.confirm('Confirm this match and create handoff?')) return;
    
    try {
      await api(`/items/lost/${lostItemId}/confirm-match/${foundItemId}`, {
        method: 'POST'
      });
      alert('Match confirmed and handoff created!');
      fetchAllMatches();
      if (activeTab === 'handoffs') fetchHandoffs();
    } catch (err) {
      console.error('Error confirming match:', err);
      alert('Failed to confirm match');
    }
  };

  const handleRejectMatch = async (foundItemId, lostItemId) => {
    // For now, just close the expanded row - in future could mark as "not a match"
    alert('Match rejection feature - to be implemented');
  };

  const handleAIAnalysis = async (lostItemId, foundItemId) => {
    const matchKey = `${lostItemId}-${foundItemId}`;
    
    setAiLoading(prev => ({ ...prev, [matchKey]: true }));
    setAiError(prev => ({ ...prev, [matchKey]: null }));
    
    try {
      const response = await api(`/items/analyze-match/${lostItemId}/${foundItemId}`, {
        method: 'POST'
      });
      
      setAiAnalysis(prev => ({ ...prev, [matchKey]: response }));
      setShowAiAnalysis(prev => ({ ...prev, [matchKey]: true }));
    } catch (err) {
      console.error('Error performing AI analysis:', err);
      
      // Determine appropriate error message based on error type
      let errorMessage = 'An unexpected error occurred. Please try again.';
      
      if (err.response) {
        // Server responded with error status
        const status = err.response.status;
        const data = err.response.data;
        
        if (status === 400) {
          errorMessage = '❌ AI analysis is not available. Please ensure Gemini AI is enabled and configured with a valid API key.';
        } else if (status === 404) {
          errorMessage = '❌ Match not found. The items may have been deleted or the match no longer exists.';
        } else if (status === 500) {
          if (typeof data === 'string' && data.includes('timeout')) {
            errorMessage = '⏱️ AI analysis timed out. The AI service is taking too long to respond. Please try again.';
          } else if (typeof data === 'string' && data.includes('API key')) {
            errorMessage = '🔑 Invalid or missing API key. Please contact the administrator to configure Gemini AI properly.';
          } else if (typeof data === 'string' && data.includes('quota')) {
            errorMessage = '📊 API quota exceeded. The daily limit for AI requests has been reached. Please try again later.';
          } else {
            errorMessage = 'AI analysis failed on the server. Please contact the administrator if this persists.';
          }
        } else if (status === 401 || status === 403) {
          errorMessage = '🔒 Unauthorized. Please log in again to continue.';
        } else {
          errorMessage = `Server error (${status}). Please try again or contact support.`;
        }
      } else if (err.request) {
        // Request was made but no response received
        errorMessage = '🌐 Network error. Please check your internet connection and try again.';
      } else if (err.message) {
        // Error in request setup
        errorMessage = `${err.message}`;
      }
      
      setAiError(prev => ({ 
        ...prev, 
        [matchKey]: errorMessage
      }));
    } finally {
      setAiLoading(prev => ({ ...prev, [matchKey]: false }));
    }
  };

  const getFilteredMatchData = () => {
    if (matchFilter === 'ALL') return matchData;
    if (matchFilter === 'MATCHED') return matchData.filter(item => item.confirmedMatches > 0);
    if (matchFilter === 'UNMATCHED') return matchData.filter(item => item.confirmedMatches === 0);
    return matchData;
  };

  // Show loading only for the active tab
  if ((activeTab === 'matching' && matchLoading) || (activeTab === 'handoffs' && loading)) {
    return <div className="admin-container"><div className="loading">Loading...</div></div>;
  }

  return (
    <div className="admin-container">
      <div className="admin-header">
        <div className="admin-header-content">
          <div className="admin-title-section">
            <h1>Admin Dashboard</h1>
            <p className="admin-subtitle">Manage matches and handoffs</p>
          </div>
          <button 
            className="mobile-menu-toggle"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            aria-label="Toggle menu"
          >
            <span className={`hamburger ${mobileMenuOpen ? 'open' : ''}`}></span>
          </button>
        </div>
        
        {/* Tab Navigation */}
        <div className={`tab-nav ${mobileMenuOpen ? 'mobile-open' : ''}`}>
          <button 
            className={`tab-button ${activeTab === 'matching' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('matching');
              setMobileMenuOpen(false);
            }}
          >
            Matching ({matchData.length} items)
          </button>
          <button 
            className={`tab-button ${activeTab === 'handoffs' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('handoffs');
              setMobileMenuOpen(false);
            }}
          >
            Handoff Queue ({handoffs.length})
          </button>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      {/* MATCHING TAB */}
      {activeTab === 'matching' && (
        <>
          <div className="filter-section">
            <label>Filter:</label>
            <select 
              value={matchFilter} 
              onChange={(e) => setMatchFilter(e.target.value)}
              className="filter-select"
            >
              <option value="ALL">All Found Items</option>
              <option value="MATCHED">With Confirmed Matches</option>
              <option value="UNMATCHED">Without Matches</option>
            </select>
          </div>

          <div className="matching-summary">
            <div className="summary-card">
              <span className="summary-icon">📦</span>
              <h3>Total Found Items</h3>
              <p className="summary-count">{matchData.length}</p>
            </div>
            <div className="summary-card">
              <span className="summary-icon">🔍</span>
              <h3>With Matches</h3>
              <p className="summary-count">
                {matchData.filter(item => item.totalMatches > 0).length}
              </p>
            </div>
            <div className="summary-card">
              <span className="summary-icon">✔</span>
              <h3>Confirmed Matches</h3>
              <p className="summary-count">
                {matchData.reduce((sum, item) => sum + item.confirmedMatches, 0)}
              </p>
            </div>
          </div>

          {/* Mobile Stats Bar */}
          <div className="mobile-stats-bar">
            <div className="mobile-stat-item">
              <span className="mobile-stat-label">Total</span>
              <span className="mobile-stat-value">{matchData.length}</span>
            </div>
            <div className="mobile-stat-item">
              <span className="mobile-stat-label">Matches</span>
              <span className="mobile-stat-value">{matchData.filter(item => item.totalMatches > 0).length}</span>
            </div>
            <div className="mobile-stat-item">
              <span className="mobile-stat-label">Confirmed</span>
              <span className="mobile-stat-value">{matchData.reduce((sum, item) => sum + item.confirmedMatches, 0)}</span>
            </div>
          </div>

          <div className="matching-table-container">
            <table className="matching-table">
              <thead>
                <tr>
                  <th></th>
                  <th>Found Item</th>
                  <th>Category</th>
                  <th>Location</th>
                  <th>Date Found</th>
                  <th>Potential Matches</th>
                  <th>Confirmed</th>
                </tr>
              </thead>
              <tbody>
                {getFilteredMatchData().length === 0 ? (
                  <tr>
                    <td colSpan="7" className="no-data">No items found</td>
                  </tr>
                ) : (
                  getFilteredMatchData().map((item) => (
                    <React.Fragment key={item.foundItem.id}>
                      <tr className="found-item-row">
                        <td>
                          <button 
                            className="expand-button"
                            onClick={() => toggleExpandItem(item.foundItem.id)}
                          >
                            {expandedItems.has(item.foundItem.id) ? '▼' : '▶'}
                          </button>
                        </td>
                        <td>
                          <strong>{item.foundItem.title}</strong>
                          <br />
                          <small>{item.foundItem.reporterName}</small>
                        </td>
                        <td>{item.foundItem.category}</td>
                        <td>{item.foundItem.location}</td>
                        <td>{new Date(item.foundItem.dateFound).toLocaleDateString()}</td>
                        <td>
                          <span className="match-count">{item.totalMatches}</span>
                        </td>
                        <td>
                          <span className="confirmed-count">{item.confirmedMatches}</span>
                        </td>
                      </tr>
                      
                      {/* Expanded matches */}
                      {expandedItems.has(item.foundItem.id) && item.matches.length > 0 && (
                        <tr className="expanded-matches-row">
                          <td colSpan="7">
                            <div className="matches-container">
                              {item.matches.filter(match => 
                                // Show only confirmed matches, or all if none are confirmed
                                match.confirmed || !item.matches.some(m => m.confirmed)
                              ).map((match, idx) => (
                                <div key={idx} className="match-card">
                                  <div className="match-header">
                                    <div>
                                      <h4>{match.lostItem.title}</h4>
                                      <p className="match-owner">Owner: {match.lostItem.ownerName} ({match.lostItem.ownerEmail})</p>
                                    </div>
                                    <div className="match-confidence">
                                      <span className={`confidence-badge ${getConfidenceBadgeClass(match.confidenceScore)}`}>
                                        {getConfidenceLabel(match.confidenceScore)} - {match.confidenceScore}%
                                      </span>
                                    </div>
                                  </div>
                                  
                                  {/* Images Section */}
                                  <div className="match-images">
                                    <div className="image-comparison">
                                      <div className="image-column">
                                        <h5>Found Item Image</h5>
                                        {item.foundItem.imageData ? (
                                          <img 
                                            src={item.foundItem.imageData} 
                                            alt={item.foundItem.title}
                                            className="match-image"
                                          />
                                        ) : (
                                          <div className="no-image">No image available</div>
                                        )}
                                      </div>
                                      <div className="image-column">
                                        <h5>Lost Item Image</h5>
                                        {match.lostItem.imageData ? (
                                          <img 
                                            src={match.lostItem.imageData} 
                                            alt={match.lostItem.title}
                                            className="match-image"
                                          />
                                        ) : (
                                          <div className="no-image">No image available</div>
                                        )}
                                      </div>
                                    </div>
                                  </div>
                                  
                                  <div className="match-details">
                                    <p><strong>Category:</strong> {match.lostItem.category} / {match.lostItem.subcategory}</p>
                                    <p><strong>Date Lost:</strong> {new Date(match.lostItem.dateLost).toLocaleDateString()}</p>
                                    <p><strong>Match Reason:</strong> {match.matchReason}</p>
                                    <p><strong>Lost Item Description:</strong> {match.lostItem.description}</p>
                                    <p><strong>Found Item Description:</strong> {item.foundItem.description}</p>
                                  </div>

                                  <div className="match-actions">
                                    {match.confirmed ? (
                                      <span className="confirmed-badge">✓ Confirmed & Handoff Created</span>
                                    ) : (
                                      <>
                                        <button 
                                          className="btn-confirm-match"
                                          onClick={() => handleConfirmMatch(item.foundItem.id, match.lostItem.id)}
                                        >
                                          ✓ Confirm & Create Handoff
                                        </button>
                                        <button 
                                          className="btn-reject-match"
                                          onClick={() => handleRejectMatch(item.foundItem.id, match.lostItem.id)}
                                        >
                                          ✗ Not a Match
                                        </button>
                                        <button 
                                          className="btn-ai-analysis"
                                          onClick={() => handleAIAnalysis(match.lostItem.id, item.foundItem.id)}
                                          disabled={aiLoading[`${match.lostItem.id}-${item.foundItem.id}`]}
                                        >
                                          {aiLoading[`${match.lostItem.id}-${item.foundItem.id}`] ? '⏳' : '🤖'} AI Analysis
                                        </button>
                                      </>
                                    )}
                                  </div>

                                  {/* AI Analysis Loading State */}
                                  {aiLoading[`${match.lostItem.id}-${item.foundItem.id}`] && (
                                    <div className="ai-loading">
                                      <div className="spinner"></div>
                                      <span>AI is analyzing this match... Please wait.</span>
                                    </div>
                                  )}

                                  {/* AI Analysis Error State */}
                                  {aiError[`${match.lostItem.id}-${item.foundItem.id}`] && (
                                    <div className="ai-error">
                                      <span className="error-icon">⚠️</span>
                                      <span>{aiError[`${match.lostItem.id}-${item.foundItem.id}`]}</span>
                                    </div>
                                  )}

                                  {/* AI Analysis Results */}
                                  {showAiAnalysis[`${match.lostItem.id}-${item.foundItem.id}`] && 
                                   aiAnalysis[`${match.lostItem.id}-${item.foundItem.id}`] && (
                                    <div className="ai-results">
                                      <div className="ai-results-header">
                                        <h4>🤖 AI Analysis Results</h4>
                                        <button 
                                          className="btn-close-ai"
                                          onClick={() => setShowAiAnalysis(prev => ({ 
                                            ...prev, 
                                            [`${match.lostItem.id}-${item.foundItem.id}`]: false 
                                          }))}
                                        >
                                          ✕
                                        </button>
                                      </div>
                                      
                                      <div className="ai-confidence">
                                        <span className="ai-label">AI Confidence Score:</span>
                                        <span className={`ai-confidence-badge ${getConfidenceBadgeClass(aiAnalysis[`${match.lostItem.id}-${item.foundItem.id}`].confidenceScore)}`}>
                                          {aiAnalysis[`${match.lostItem.id}-${item.foundItem.id}`].confidenceScore}%
                                        </span>
                                      </div>

                                      <div className="ai-reasoning">
                                        <h5>Reasoning:</h5>
                                        <p>{aiAnalysis[`${match.lostItem.id}-${item.foundItem.id}`].reasoning}</p>
                                      </div>

                                      {aiAnalysis[`${match.lostItem.id}-${item.foundItem.id}`].matchingFeatures && 
                                       aiAnalysis[`${match.lostItem.id}-${item.foundItem.id}`].matchingFeatures.length > 0 && (
                                        <div className="ai-features">
                                          <h5>✓ Matching Features:</h5>
                                          <ul>
                                            {aiAnalysis[`${match.lostItem.id}-${item.foundItem.id}`].matchingFeatures.map((feature, i) => (
                                              <li key={i}>{feature}</li>
                                            ))}
                                          </ul>
                                        </div>
                                      )}

                                      {aiAnalysis[`${match.lostItem.id}-${item.foundItem.id}`].discrepancies && 
                                       aiAnalysis[`${match.lostItem.id}-${item.foundItem.id}`].discrepancies.length > 0 && (
                                        <div className="ai-discrepancies">
                                          <h5>⚠ Discrepancies:</h5>
                                          <ul>
                                            {aiAnalysis[`${match.lostItem.id}-${item.foundItem.id}`].discrepancies.map((discrepancy, i) => (
                                              <li key={i}>{discrepancy}</li>
                                            ))}
                                          </ul>
                                        </div>
                                      )}
                                    </div>
                                  )}
                                </div>
                              ))}
                            </div>
                          </td>
                        </tr>
                      )}
                      
                      {expandedItems.has(item.foundItem.id) && item.matches.length === 0 && (
                        <tr className="expanded-matches-row">
                          <td colSpan="7">
                            <div className="no-matches">No potential matches found for this item</div>
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </>
      )}

      {/* HANDOFF QUEUE TAB */}
      {activeTab === 'handoffs' && (
        <>
          <div className="filter-section">
            <label>Filter by Status:</label>
            <select 
              value={filter} 
              onChange={(e) => setFilter(e.target.value)}
              className="filter-select"
            >
              <option value="ALL">All Handoffs</option>
              <option value="PENDING">Pending</option>
              <option value="SCHEDULED">Scheduled</option>
              <option value="COMPLETED">Completed</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </div>

          <div className="handoffs-summary">
        <div className="summary-card">
          <span className="summary-icon">📋</span>
          <h3>Total Handoffs</h3>
          <p className="summary-count">{handoffs.length}</p>
        </div>
        <div className="summary-card">
          <span className="summary-icon">⏳</span>
          <h3>Pending</h3>
          <p className="summary-count">
            {handoffs.filter(h => h.status === 'PENDING').length}
          </p>
        </div>
        <div className="summary-card">
          <span className="summary-icon">📅</span>
          <h3>Scheduled</h3>
          <p className="summary-count">
            {handoffs.filter(h => h.status === 'SCHEDULED').length}
          </p>
        </div>
        <div className="summary-card">
          <span className="summary-icon">✅</span>
          <h3>Completed</h3>
          <p className="summary-count">
            {handoffs.filter(h => h.status === 'COMPLETED').length}
          </p>
        </div>
      </div>

      {/* Mobile Stats Bar */}
      <div className="mobile-stats-bar">
        <div className="mobile-stat-item">
          <span className="mobile-stat-label">Total</span>
          <span className="mobile-stat-value">{handoffs.length}</span>
        </div>
        <div className="mobile-stat-item">
          <span className="mobile-stat-label">Pending</span>
          <span className="mobile-stat-value">{handoffs.filter(h => h.status === 'PENDING').length}</span>
        </div>
        <div className="mobile-stat-item">
          <span className="mobile-stat-label">Scheduled</span>
          <span className="mobile-stat-value">{handoffs.filter(h => h.status === 'SCHEDULED').length}</span>
        </div>
        <div className="mobile-stat-item">
          <span className="mobile-stat-label">Done</span>
          <span className="mobile-stat-value">{handoffs.filter(h => h.status === 'COMPLETED').length}</span>
        </div>
      </div>

      <div className="handoffs-table-container">
        <table className="handoffs-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Lost Item</th>
              <th>Found Item</th>
              <th>Status</th>
              <th>Scheduled Time</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {handoffs.length === 0 ? (
              <tr>
                <td colSpan="6" className="no-data">No handoffs found</td>
              </tr>
            ) : (
              handoffs.map((handoff) => (
                <tr key={handoff.id}>
                  <td>{handoff.id}</td>
                  <td>
                    <div className="item-info">
                      <strong>{handoff.lostItem?.title || 'N/A'}</strong>
                      <br />
                      <small>Owner: {handoff.lostItem?.ownerName || 'Unknown'}</small>
                    </div>
                  </td>
                  <td>
                    <div className="item-info">
                      <strong>{handoff.foundItem?.title || 'N/A'}</strong>
                      <br />
                      <small>Reporter: {handoff.foundItem?.reporterName || 'Unknown'}</small>
                    </div>
                  </td>
                  <td>
                    <span className={`status-badge ${getStatusBadgeClass(handoff.status)}`}>
                      {handoff.status}
                    </span>
                  </td>
                  <td>
                    <small>{formatDate(handoff.scheduledHandoffTime)}</small>
                  </td>
                  <td>
                    <div className="action-buttons">
                      {handoff.status !== 'COMPLETED' ? (
                        <>
                          <button 
                            className="btn-options"
                            onClick={(e) => toggleOptionsMenu(handoff.id, e)}
                            title="Options"
                          >
                            ⋮
                          </button>
                          {openOptionsMenu === handoff.id && (
                            <div 
                              className="options-dropdown-fixed"
                              style={{
                                top: `${menuPosition.top}px`,
                                left: `${menuPosition.left}px`,
                                transform: menuPosition.transform || 'none'
                              }}
                            >
                              {handoff.status === 'PENDING' && (
                                <button onClick={() => handleScheduleClick(handoff, 'schedule')}>
                                  📅 Schedule
                                </button>
                              )}
                              {handoff.status === 'SCHEDULED' && (
                                <button onClick={() => handleScheduleClick(handoff, 'reschedule')}>
                                  🔄 Reschedule
                                </button>
                              )}
                              {handoff.status === 'SCHEDULED' && (
                                <button onClick={() => handleCancelHandoff(handoff)}>
                                  ↩️ Cancel (Back to Pending)
                                </button>
                              )}
                              {handoff.status === 'SCHEDULED' && (
                                <button onClick={() => handleCompleteHandoff(handoff)} className="success">
                                  ✅ Complete Handoff
                                </button>
                              )}
                              <button 
                                onClick={() => handleRejectHandoff(handoff.id)}
                                className="danger"
                              >
                                ❌ Reject (Not a Match)
                              </button>
                            </div>
                          )}
                        </>
                      ) : (
                        <span className="no-actions">—</span>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
          </div>

          {/* Modal for viewing/editing handoff details */}
      {showModal && selectedHandoff && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Handoff Details - #{selectedHandoff.id}</h2>
              <button className="modal-close" onClick={() => setShowModal(false)}>&times;</button>
            </div>
            
            <div className="modal-body">
              <div className="handoff-details-section">
                <h3>Item Information</h3>
                <div className="details-grid">
                  <div className="detail-item">
                    <strong>Lost Item:</strong> {selectedHandoff.lostItem?.title || 'N/A'}
                  </div>
                  <div className="detail-item">
                    <strong>Owner:</strong> {selectedHandoff.lostItem?.ownerName || 'Unknown'} ({selectedHandoff.lostItem?.ownerEmail || 'N/A'})
                  </div>
                  <div className="detail-item">
                    <strong>Found Item:</strong> {selectedHandoff.foundItem?.title || 'N/A'}
                  </div>
                  <div className="detail-item">
                    <strong>Reporter:</strong> {selectedHandoff.foundItem?.reporterName || 'Unknown'} ({selectedHandoff.foundItem?.reporterEmail || 'N/A'})
                  </div>
                  <div className="detail-item">
                    <strong>Location:</strong> {selectedHandoff.foundItem?.location || 'N/A'}
                  </div>
                </div>
              </div>

              <form onSubmit={handleUpdateHandoff} className="update-form">
                <h3>Update Handoff</h3>
                
                <div className="form-group">
                  <label>Status</label>
                  <select 
                    value={updateForm.status} 
                    onChange={(e) => setUpdateForm({...updateForm, status: e.target.value})}
                    required
                  >
                    <option value="PENDING">Pending</option>
                    <option value="SCHEDULED">Scheduled</option>
                    <option value="COMPLETED">Completed</option>
                    <option value="CANCELLED">Cancelled</option>
                  </select>
                </div>

                <div className="form-group">
                  <label>Assigned To</label>
                  <input 
                    type="text" 
                    value={updateForm.assignedTo}
                    onChange={(e) => setUpdateForm({...updateForm, assignedTo: e.target.value})}
                    placeholder="Admin username"
                  />
                </div>

                <div className="form-group">
                  <label>Scheduled Handoff Time</label>
                  <input 
                    type="datetime-local" 
                    value={updateForm.scheduledHandoffTime}
                    onChange={(e) => setUpdateForm({...updateForm, scheduledHandoffTime: e.target.value})}
                  />
                </div>

                <div className="form-group">
                  <label>Handoff Location</label>
                  <input 
                    type="text" 
                    value={updateForm.handoffLocation}
                    onChange={(e) => setUpdateForm({...updateForm, handoffLocation: e.target.value})}
                    placeholder="e.g., Main Office, Room 101"
                  />
                </div>

                <div className="form-group">
                  <label>Notes</label>
                  <textarea 
                    value={updateForm.notes}
                    onChange={(e) => setUpdateForm({...updateForm, notes: e.target.value})}
                    rows="3"
                    placeholder="Additional notes or instructions"
                  />
                </div>

                {updateForm.status === 'CANCELLED' && (
                  <div className="form-group">
                    <label>Cancellation Reason</label>
                    <textarea 
                      value={updateForm.cancellationReason}
                      onChange={(e) => setUpdateForm({...updateForm, cancellationReason: e.target.value})}
                      rows="2"
                      placeholder="Why was this handoff cancelled?"
                    />
                  </div>
                )}

                <div className="modal-actions">
                  <button type="submit" className="btn-save">Save Changes</button>
                  <button type="button" className="btn-cancel" onClick={() => setShowModal(false)}>
                    Cancel
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
          )}

          {/* Schedule/Reschedule Modal */}
          {showScheduleModal && selectedHandoff && (
            <div className="modal-overlay" onClick={() => setShowScheduleModal(false)}>
              <div className="modal-content schedule-modal" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                  <h2>{scheduleAction === 'schedule' ? '📅 Schedule' : '🔄 Reschedule'} Handoff - #{selectedHandoff.id}</h2>
                  <button className="modal-close" onClick={() => setShowScheduleModal(false)}>&times;</button>
                </div>
                
                <div className="modal-body">
                  <div className="handoff-details-section">
                    <h3>Item Information</h3>
                    <div className="details-grid">
                      <div className="detail-item">
                        <strong>Lost Item:</strong> {selectedHandoff.lostItem?.title || 'N/A'}
                      </div>
                      <div className="detail-item">
                        <strong>Owner:</strong> {selectedHandoff.lostItem?.ownerName || 'Unknown'}
                      </div>
                      <div className="detail-item">
                        <strong>Found Item:</strong> {selectedHandoff.foundItem?.title || 'N/A'}
                      </div>
                      <div className="detail-item">
                        <strong>Reporter:</strong> {selectedHandoff.foundItem?.reporterName || 'Unknown'}
                      </div>
                    </div>
                  </div>

                  <form onSubmit={handleScheduleSubmit} className="update-form">
                    <h3>{scheduleAction === 'schedule' ? 'Schedule Details' : 'Update Schedule'}</h3>
                    
                    <div className="form-group">
                      <label>Scheduled Handoff Time *</label>
                      <input 
                        type="datetime-local" 
                        value={scheduleForm.scheduledHandoffTime}
                        onChange={(e) => setScheduleForm({...scheduleForm, scheduledHandoffTime: e.target.value})}
                        required
                      />
                    </div>

                    <div className="form-group">
                      <label>Handoff Location *</label>
                      <input 
                        type="text" 
                        value={scheduleForm.handoffLocation}
                        onChange={(e) => setScheduleForm({...scheduleForm, handoffLocation: e.target.value})}
                        placeholder="e.g., Main Office, Room 101"
                        required
                      />
                    </div>

                    <div className="form-group">
                      <label>Assigned To</label>
                      <input 
                        type="text" 
                        value={scheduleForm.assignedTo}
                        onChange={(e) => setScheduleForm({...scheduleForm, assignedTo: e.target.value})}
                        placeholder="Admin username (optional)"
                      />
                    </div>

                    <div className="modal-actions">
                      <button type="submit" className="btn-save">
                        {scheduleAction === 'schedule' ? '📅 Schedule' : '🔄 Reschedule'}
                      </button>
                      <button type="button" className="btn-cancel" onClick={() => setShowScheduleModal(false)}>
                        Cancel
                      </button>
                    </div>
                  </form>
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default AdminDashboard;
