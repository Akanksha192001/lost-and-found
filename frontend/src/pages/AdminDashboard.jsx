import React, { useState, useEffect } from 'react';
import { api } from '../api/client';
import './Admin.css';

const AdminDashboard = () => {
  const [activeTab, setActiveTab] = useState('matching'); // 'matching' or 'handoffs'
  
  // Matching state
  const [matchData, setMatchData] = useState([]);
  const [matchLoading, setMatchLoading] = useState(true);
  const [matchFilter, setMatchFilter] = useState('ALL'); // ALL, MATCHED, UNMATCHED
  const [expandedItems, setExpandedItems] = useState(new Set());
  
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
      setMenuPosition({
        top: buttonRect.bottom + window.scrollY,
        left: buttonRect.right + window.scrollX - 200 // 200px is the menu width
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
        <h1>Admin Dashboard</h1>
        
        {/* Tab Navigation */}
        <div className="tab-nav">
          <button 
            className={`tab-button ${activeTab === 'matching' ? 'active' : ''}`}
            onClick={() => setActiveTab('matching')}
          >
            Matching ({matchData.length} items)
          </button>
          <button 
            className={`tab-button ${activeTab === 'handoffs' ? 'active' : ''}`}
            onClick={() => setActiveTab('handoffs')}
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
              <h3>Total Found Items</h3>
              <p className="summary-count">{matchData.length}</p>
            </div>
            <div className="summary-card">
              <h3>With Matches</h3>
              <p className="summary-count pending">
                {matchData.filter(item => item.totalMatches > 0).length}
              </p>
            </div>
            <div className="summary-card">
              <h3>Confirmed Matches</h3>
              <p className="summary-count completed">
                {matchData.reduce((sum, item) => sum + item.confirmedMatches, 0)}
              </p>
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
                                      </>
                                    )}
                                  </div>
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
          <h3>Total Handoffs</h3>
          <p className="summary-count">{handoffs.length}</p>
        </div>
        <div className="summary-card">
          <h3>Pending</h3>
          <p className="summary-count pending">
            {handoffs.filter(h => h.status === 'PENDING').length}
          </p>
        </div>
        <div className="summary-card">
          <h3>Scheduled</h3>
          <p className="summary-count scheduled">
            {handoffs.filter(h => h.status === 'SCHEDULED').length}
          </p>
        </div>
        <div className="summary-card">
          <h3>Completed</h3>
          <p className="summary-count completed">
            {handoffs.filter(h => h.status === 'COMPLETED').length}
          </p>
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
                      <strong>{handoff.lostItem.title}</strong>
                      <br />
                      <small>Owner: {handoff.lostItem.ownerName}</small>
                    </div>
                  </td>
                  <td>
                    <div className="item-info">
                      <strong>{handoff.foundItem.title}</strong>
                      <br />
                      <small>Reporter: {handoff.foundItem.reporterName}</small>
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
                                left: `${menuPosition.left}px`
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
                    <strong>Lost Item:</strong> {selectedHandoff.lostItem.title}
                  </div>
                  <div className="detail-item">
                    <strong>Owner:</strong> {selectedHandoff.lostItem.ownerName} ({selectedHandoff.lostItem.ownerEmail})
                  </div>
                  <div className="detail-item">
                    <strong>Found Item:</strong> {selectedHandoff.foundItem.title}
                  </div>
                  <div className="detail-item">
                    <strong>Reporter:</strong> {selectedHandoff.foundItem.reporterName} ({selectedHandoff.foundItem.reporterEmail})
                  </div>
                  <div className="detail-item">
                    <strong>Location:</strong> {selectedHandoff.foundItem.location}
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
                        <strong>Lost Item:</strong> {selectedHandoff.lostItem.title}
                      </div>
                      <div className="detail-item">
                        <strong>Owner:</strong> {selectedHandoff.lostItem.ownerName}
                      </div>
                      <div className="detail-item">
                        <strong>Found Item:</strong> {selectedHandoff.foundItem.title}
                      </div>
                      <div className="detail-item">
                        <strong>Reporter:</strong> {selectedHandoff.foundItem.reporterName}
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
