package neiu.lostfound.dto;

import jakarta.validation.constraints.NotBlank;

public class LostItemRequest {
    @NotBlank
    public String title;
    public String description;
    @NotBlank
    public String location;
    public String dateLost; // ISO yyyy-MM-dd
    public String imageUrl;
    public String ownerName;
    public String ownerEmail;
    public String ownerAddress;
    public Long reportedBy; // User ID
    public Long matchedWith; // FoundItem ID (nullable)
    public String category;
    public String subcategory;
}
