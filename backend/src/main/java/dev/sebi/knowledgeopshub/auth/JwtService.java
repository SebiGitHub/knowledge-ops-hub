package dev.sebi.knowledgeopshub.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

  private final Algorithm algorithm;
  private final JWTVerifier verifier;
  private final String issuer;
  private final long accessTokenMinutes;

  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.issuer}") String issuer,
      @Value("${app.jwt.accessTokenMinutes}") long accessTokenMinutes
  ) {
    this.algorithm = Algorithm.HMAC256(secret);
    this.verifier = JWT.require(algorithm).withIssuer(issuer).build();
    this.issuer = issuer;
    this.accessTokenMinutes = accessTokenMinutes;
  }

  public String createAccessToken(UserEntity user) {
    Instant now = Instant.now();
    Instant exp = now.plus(accessTokenMinutes, ChronoUnit.MINUTES);

    return JWT.create()
        .withIssuer(issuer)
        .withSubject(user.getId().toString())
        .withClaim("email", user.getEmail())
        .withClaim("role", user.getRole().name())
        .withIssuedAt(Date.from(now))
        .withExpiresAt(Date.from(exp))
        .sign(algorithm);
  }

  public DecodedJWT verify(String token) throws JWTVerificationException {
    return verifier.verify(token);
  }

  public UUID subjectAsUserId(DecodedJWT jwt) {
    return UUID.fromString(jwt.getSubject());
  }
}
