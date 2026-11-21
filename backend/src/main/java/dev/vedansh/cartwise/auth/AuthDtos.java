package dev.vedansh.cartwise.auth;

import dev.vedansh.cartwise.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 80) String name,
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Size(min = 8, max = 72, message = "must be 8-72 characters") String password) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record UserView(Long id, String name, String email, Role role) {
    }

    public record AuthResponse(String token, UserView user) {
    }
}
