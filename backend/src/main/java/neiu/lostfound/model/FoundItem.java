package neiu.lostfound.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "found_items")
public class FoundItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String location;

    @Temporal(TemporalType.DATE)
    private Date dateFound;

    @Column(length = 255)
    private String imageUrl;

    @Column(length = 100)
    private String reporterName;

    @Column(length = 100)
    private String reporterEmail;

    @Column(length = 255)
    private String reporterAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by")
    private User reportedBy;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_with")
    private LostItem matchedWith;

    @Column(length = 255)
    private String keywords; // comma-separated keywords for matching

    @Column(length = 100)
    private String category;

    @Column(length = 100)
    private String subcategory;

    public enum Status {
        UNCLAIMED, MATCHED, RETURNED
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Date getDateFound() { return dateFound; }
    public void setDateFound(Date dateFound) { this.dateFound = dateFound; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    public String getReporterEmail() { return reporterEmail; }
    public void setReporterEmail(String reporterEmail) { this.reporterEmail = reporterEmail; }
    public String getReporterAddress() { return reporterAddress; }
    public void setReporterAddress(String reporterAddress) { this.reporterAddress = reporterAddress; }
    public User getReportedBy() { return reportedBy; }
    public void setReportedBy(User reportedBy) { this.reportedBy = reportedBy; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LostItem getMatchedWith() { return matchedWith; }
    public void setMatchedWith(LostItem matchedWith) { this.matchedWith = matchedWith; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSubcategory() { return subcategory; }
    public void setSubcategory(String subcategory) { this.subcategory = subcategory; }
}
