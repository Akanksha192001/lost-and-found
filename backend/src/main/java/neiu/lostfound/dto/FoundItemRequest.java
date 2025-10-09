package neiu.lostfound.dto;

import jakarta.validation.constraints.NotBlank;

public class FoundItemRequest {
  @NotBlank
  public String title;
  public String description;
  @NotBlank
  public String category;
  @NotBlank
  public String locationFound;
  public String dateFound; // ISO yyyy-MM-dd or datetime
  public String photoData; // base64 encoded image string
}
