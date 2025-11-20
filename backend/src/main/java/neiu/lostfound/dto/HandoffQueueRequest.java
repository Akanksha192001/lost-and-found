package neiu.lostfound.dto;

import java.util.Date;

public class HandoffQueueRequest {
    private Long lostItemId;
    private Long foundItemId;
    private Long matchId;
    private String assignedTo;
    private Date scheduledHandoffTime;
    private String handoffLocation;
    private String notes;
    private String status;
    private String cancellationReason;

    // Getters and setters
    public Long getLostItemId() {
        return lostItemId;
    }

    public void setLostItemId(Long lostItemId) {
        this.lostItemId = lostItemId;
    }

    public Long getFoundItemId() {
        return foundItemId;
    }

    public void setFoundItemId(Long foundItemId) {
        this.foundItemId = foundItemId;
    }

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public Date getScheduledHandoffTime() {
        return scheduledHandoffTime;
    }

    public void setScheduledHandoffTime(Date scheduledHandoffTime) {
        this.scheduledHandoffTime = scheduledHandoffTime;
    }

    public String getHandoffLocation() {
        return handoffLocation;
    }

    public void setHandoffLocation(String handoffLocation) {
        this.handoffLocation = handoffLocation;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
}
