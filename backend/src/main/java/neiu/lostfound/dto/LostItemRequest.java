package neiu.lostfound.dto;

import jakarta.validation.constraints.NotBlank;

public class LostItemRequest {
  @NotBlank
  public String title;
  public String description;
  @NotBlank
  public String locationLastSeen;
  public String dateLost; // ISO yyyy-MM-dd or datetime
}
