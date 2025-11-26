package neiu.lostfound.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "item_matches")
public class ItemMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lost_item_id")
    private Long lostItemId;

    @Column(name = "found_item_id")
    private Long foundItemId;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Status status; // TENTATIVE, CONFIRMED

    @Column(length = 100)
    private String matchedBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date matchedAt;

    public enum Status {
        TENTATIVE, CONFIRMED
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getLostItemId() { return lostItemId; }
    public void setLostItemId(Long lostItemId) { this.lostItemId = lostItemId; }
    public Long getFoundItemId() { return foundItemId; }
    public void setFoundItemId(Long foundItemId) { this.foundItemId = foundItemId; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getMatchedBy() { return matchedBy; }
    public void setMatchedBy(String matchedBy) { this.matchedBy = matchedBy; }
    public Date getMatchedAt() { return matchedAt; }
    public void setMatchedAt(Date matchedAt) { this.matchedAt = matchedAt; }
}