package pl.chrono.usermanager.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.usermanager.dto.request.AuthRequest;
import pl.chrono.usermanager.dto.response.AuthResponse;
import pl.chrono.usermanager.security.JwtService;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;
  private final UserRepository userRepository;

  public AuthResponse login(AuthRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.getEmail(),
            request.getPassword()
        )
    );

    var user = userRepository.findByEmail(request.getEmail()).orElseThrow(
        () -> new IllegalArgumentException("User not found"));
    Map<String, Object> extraClaims = new HashMap<>();
    extraClaims.put("userId", user.getUserId());
    UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
    String jwtToken = jwtService.generateToken(extraClaims, userDetails);

    String refreshToken = UUID.randomUUID().toString();

    return new AuthResponse(jwtToken, refreshToken);
  }

  public Integer extractUserIdFromToken(String token) {
    return jwtService.extractClaim(token, claims -> claims.get("userId", Integer.class));
  }
}
