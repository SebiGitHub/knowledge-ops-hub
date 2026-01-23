package dev.sebi.knowledgeopshub.auth;

import dev.sebi.knowledgeopshub.auth.dto.MeResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class MeController {

  @GetMapping("/me")
  public MeResponse me(Authentication auth) {
    // lo rellenaremos con JwtAuthFilter (id/email/role)
    JwtPrincipal p = (JwtPrincipal) auth.getPrincipal();
    return new MeResponse(p.userId(), p.email(), p.role());
  }

  public record JwtPrincipal(UUID userId, String email, UserRole role) {}
}
