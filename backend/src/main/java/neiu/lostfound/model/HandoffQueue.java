package neiu.lostfound.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "handoff_queue")
public class HandoffQueue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lost_item_id", nullable = true)
    private LostItem lostItem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "found_item_id", nullable = true)
    private FoundItem foundItem;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "match_id")
    private ItemMatch itemMatch;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private HandoffStatus status = HandoffStatus.PENDING;

    @Column(length = 100)
    private String initiatedBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date initiatedAt;

    @Column(length = 100)
    private String assignedTo; // Admin user assigned to handle the handoff

    @Temporal(TemporalType.TIMESTAMP)
    private Date scheduledHandoffTime;

    @Column(length = 255)
    private String handoffLocation;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(length = 100)
    private String completedBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date completedAt;

    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    public enum HandoffStatus {
        PENDING,        // Initial state - waiting for admin action
        SCHEDULED,      // Handoff time and location set
        COMPLETED,      // Item successfully handed off
        CANCELLED       // Handoff cancelled (can be rescheduled)
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LostItem getLostItem() {
        return lostItem;
    }

    public void setLostItem(LostItem lostItem) {
        this.lostItem = lostItem;
    }

    public FoundItem getFoundItem() {
        return foundItem;
    }

    public void setFoundItem(FoundItem foundItem) {
        this.foundItem = foundItem;
    }

    public ItemMatch getItemMatch() {
        return itemMatch;
    }

    public void setItemMatch(ItemMatch itemMatch) {
        this.itemMatch = itemMatch;
    }

    public HandoffStatus getStatus() {
        return status;
    }

    public void setStatus(HandoffStatus status) {
        this.status = status;
    }

    public String getInitiatedBy() {
        return initiatedBy;
    }

    public void setInitiatedBy(String initiatedBy) {
        this.initiatedBy = initiatedBy;
    }

    public Date getInitiatedAt() {
        return initiatedAt;
    }

    public void setInitiatedAt(Date initiatedAt) {
        this.initiatedAt = initiatedAt;
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

    public String getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(String completedBy) {
        this.completedBy = completedBy;
    }

    public Date getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
}
