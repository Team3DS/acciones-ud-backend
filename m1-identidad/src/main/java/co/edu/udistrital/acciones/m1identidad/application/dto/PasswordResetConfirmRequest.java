package co.edu.udistrital.acciones.m1identidad.application.dto;

import co.edu.udistrital.acciones.m1identidad.application.service.PasswordPolicy;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank @Email @Size(max = 254) String correo,
        @NotBlank @Pattern(regexp = "[0-9]{6}") String code,
        @NotBlank String newPassword) {
    @JsonIgnore
    @AssertTrue(message = "La contraseña debe tener al menos 8 caracteres y como máximo 72 bytes UTF-8")
    public boolean isPasswordValid() {
        return PasswordPolicy.isValidNewPassword(newPassword);
    }

    @Override
    public String toString() { return "PasswordResetConfirmRequest[redacted]"; }
}
