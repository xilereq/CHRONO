package pl.chrono.usermanager.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

  private JwtService jwtService;
  private UserDetails userDetails;

  @BeforeEach
  void setUp() {
    jwtService = new JwtService();
    String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    long expiration = 1000 * 60 * 60 * 24; // 1 dzień w milisekundach

    ReflectionTestUtils.setField(jwtService, "secretKey", secret);
    ReflectionTestUtils.setField(jwtService, "jwtExpiration", expiration);

    userDetails = org.springframework.security.core.userdetails.User.builder()
        .username("test@test.pl")
        .password("password")
        .authorities("ROLE_USER")
        .build();
  }

  @Test
  void generateToken_ShouldGenerateValidJwtToken() {
    String token = jwtService.generateToken(userDetails);

    assertNotNull(token);
    String username = jwtService.extractUsername(token);
    assertEquals("test@test.pl", username);
  }

  @Test
  void generateTokenWithExtraClaims_ShouldIncludeExtraClaims() {
    Map<String, Object> extraClaims = new HashMap<>();
    extraClaims.put("customClaim", "customValue");

    String token = jwtService.generateToken(extraClaims, userDetails);

    assertNotNull(token);
    assertEquals("test@test.pl", jwtService.extractUsername(token));
    String customClaim = jwtService.extractClaim(token, claims -> claims.get("customClaim", String.class));
    assertEquals("customValue", customClaim);
  }

  @Test
  void isTokenValid_ShouldReturnTrueForValidToken() {
    String token = jwtService.generateToken(userDetails);

    boolean isValid = jwtService.isTokenValid(token, userDetails);

    assertTrue(isValid);
  }

  @Test
  void isTokenValid_ShouldReturnFalseForDifferentUser() {
    String token = jwtService.generateToken(userDetails);
    UserDetails differentUser = org.springframework.security.core.userdetails.User.builder()
        .username("other@test.pl")
        .password("password")
        .authorities("ROLE_USER")
        .build();

    boolean isValid = jwtService.isTokenValid(token, differentUser);

    assertFalse(isValid);
  }
}
