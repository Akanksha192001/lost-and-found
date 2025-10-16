package neiu.lostfound.model;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "returned_items")
public class ReturnedItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String location;

    @Temporal(TemporalType.DATE)
    private Date dateReturned;

    @Column(length = 255)
    private String imageUrl;

    @Column(length = 100)
    private String ownerName;

    @Column(length = 100)
    private String ownerEmail;

    @Column(length = 255)
    private String ownerAddress;

    @Column(length = 100)
    private String reporterName;

    @Column(length = 100)
    private String reporterEmail;

    @Column(length = 255)
    private String reporterAddress;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Date getDateReturned() { return dateReturned; }
    public void setDateReturned(Date dateReturned) { this.dateReturned = dateReturned; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
    public String getOwnerAddress() { return ownerAddress; }
    public void setOwnerAddress(String ownerAddress) { this.ownerAddress = ownerAddress; }
    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    public String getReporterEmail() { return reporterEmail; }
    public void setReporterEmail(String reporterEmail) { this.reporterEmail = reporterEmail; }
    public String getReporterAddress() { return reporterAddress; }
    public void setReporterAddress(String reporterAddress) { this.reporterAddress = reporterAddress; }
}