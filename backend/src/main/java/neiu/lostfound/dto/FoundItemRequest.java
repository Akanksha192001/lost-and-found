package neiu.lostfound.dto;

import jakarta.validation.constraints.NotBlank;

public class FoundItemRequest {
    @NotBlank
    public String title;
    public String description;
    @NotBlank
    public String location;
    public String dateFound; // ISO yyyy-MM-dd
    public String imageData; // Base64 encoded image data from file upload
    public String reporterName;
    public String reporterEmail;
    public Long reportedBy; // User ID
    public Long matchedWith; // LostItem ID (nullable)
    public String category;
    public String subcategory;
}
