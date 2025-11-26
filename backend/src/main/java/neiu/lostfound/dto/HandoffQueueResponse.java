package neiu.lostfound.dto;

import neiu.lostfound.model.HandoffQueue;
import neiu.lostfound.model.LostItem;
import neiu.lostfound.model.FoundItem;
import neiu.lostfound.model.ItemMatch;

public class HandoffQueueResponse {
    private Long id;
    private Long matchId;
    private HandoffQueue.HandoffStatus status;
    private String initiatedBy;
    private java.util.Date initiatedAt;
    private String assignedTo;
    private java.util.Date scheduledHandoffTime;
    private String handoffLocation;
    private String notes;
    private String completedBy;
    private java.util.Date completedAt;
    private String cancellationReason;
    
    // Embedded item data
    private LostItem lostItem;
    private FoundItem foundItem;
    private ItemMatch itemMatch;

    public HandoffQueueResponse() {}

    public HandoffQueueResponse(HandoffQueue handoff, ItemMatch match, LostItem lostItem, FoundItem foundItem) {
        this.id = handoff.getId();
        this.matchId = handoff.getMatchId();
        this.status = handoff.getStatus();
        this.initiatedBy = handoff.getInitiatedBy();
        this.initiatedAt = handoff.getInitiatedAt();
        this.assignedTo = handoff.getAssignedTo();
        this.scheduledHandoffTime = handoff.getScheduledHandoffTime();
        this.handoffLocation = handoff.getHandoffLocation();
        this.notes = handoff.getNotes();
        this.completedBy = handoff.getCompletedBy();
        this.completedAt = handoff.getCompletedAt();
        this.cancellationReason = handoff.getCancellationReason();
        this.itemMatch = match;
        this.lostItem = lostItem;
        this.foundItem = foundItem;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getMatchId() { return matchId; }
    public void setMatchId(Long matchId) { this.matchId = matchId; }
    
    public HandoffQueue.HandoffStatus getStatus() { return status; }
    public void setStatus(HandoffQueue.HandoffStatus status) { this.status = status; }
    
    public String getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(String initiatedBy) { this.initiatedBy = initiatedBy; }
    
    public java.util.Date getInitiatedAt() { return initiatedAt; }
    public void setInitiatedAt(java.util.Date initiatedAt) { this.initiatedAt = initiatedAt; }
    
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    
    public java.util.Date getScheduledHandoffTime() { return scheduledHandoffTime; }
    public void setScheduledHandoffTime(java.util.Date scheduledHandoffTime) { this.scheduledHandoffTime = scheduledHandoffTime; }
    
    public String getHandoffLocation() { return handoffLocation; }
    public void setHandoffLocation(String handoffLocation) { this.handoffLocation = handoffLocation; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getCompletedBy() { return completedBy; }
    public void setCompletedBy(String completedBy) { this.completedBy = completedBy; }
    
    public java.util.Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(java.util.Date completedAt) { this.completedAt = completedAt; }
    
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    
    public LostItem getLostItem() { return lostItem; }
    public void setLostItem(LostItem lostItem) { this.lostItem = lostItem; }
    
    public FoundItem getFoundItem() { return foundItem; }
    public void setFoundItem(FoundItem foundItem) { this.foundItem = foundItem; }
    
    public ItemMatch getItemMatch() { return itemMatch; }
    public void setItemMatch(ItemMatch itemMatch) { this.itemMatch = itemMatch; }
}
