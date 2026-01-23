package dev.sebi.knowledgeopshub.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import dev.sebi.knowledgeopshub.auth.MeController.JwtPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  public JwtAuthFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {

    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
      chain.doFilter(request, response);
      return;
    }

    String token = header.substring("Bearer ".length()).trim();

    try {
      DecodedJWT jwt = jwtService.verify(token);

      UUID userId = UUID.fromString(jwt.getSubject());
      String email = jwt.getClaim("email").asString();
      UserRole role = UserRole.valueOf(jwt.getClaim("role").asString());

      var principal = new JwtPrincipal(userId, email, role);
      var auth = new UsernamePasswordAuthenticationToken(
          principal,
          null,
          List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
      );

      SecurityContextHolder.getContext().setAuthentication(auth);
    } catch (Exception ignored) {
      SecurityContextHolder.clearContext();
    }

    chain.doFilter(request, response);
  }
}
