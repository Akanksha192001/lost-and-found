package neiu.lostfound.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RegisterRequest {
  @NotBlank
  public String name;
  @Email @NotBlank
  public String email;
  @NotBlank
  public String password;
  @NotBlank
  @Pattern(regexp = "USER", message = "Role must be USER")
  public String role;
}
