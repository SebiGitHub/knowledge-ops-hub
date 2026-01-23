package dev.sebi.knowledgeopshub.auth;

import dev.sebi.knowledgeopshub.auth.dto.AuthResponse;
import dev.sebi.knowledgeopshub.auth.dto.LoginRequest;
import dev.sebi.knowledgeopshub.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public AuthResponse register(@RequestBody @Valid RegisterRequest req) {
    String email = req.email().trim().toLowerCase();

    if (userRepository.existsByEmail(email)) {
      throw new IllegalArgumentException("Email ya registrado");
    }

    UserEntity u = new UserEntity();
    u.setEmail(email);
    u.setPasswordHash(passwordEncoder.encode(req.password()));
    u.setRole(UserRole.USER);

    UserEntity saved = userRepository.save(u);
    return new AuthResponse(jwtService.createAccessToken(saved));
  }

  @PostMapping("/login")
  public AuthResponse login(@RequestBody @Valid LoginRequest req) {
    String email = req.email().trim().toLowerCase();

    UserEntity u = userRepository.findByEmail(email)
        .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

    if (!passwordEncoder.matches(req.password(), u.getPasswordHash())) {
      throw new IllegalArgumentException("Credenciales inválidas");
    }

    return new AuthResponse(jwtService.createAccessToken(u));
  }
}
