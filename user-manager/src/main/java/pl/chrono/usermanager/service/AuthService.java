package pl.chrono.usermanager.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import pl.chrono.usermanager.dto.request.AuthRequest;
import pl.chrono.usermanager.dto.response.AuthResponse;
import pl.chrono.usermanager.security.JwtService;

import java.util.UUID;

@Service
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;

  public AuthService(AuthenticationManager authenticationManager, JwtService jwtService, UserDetailsService userDetailsService) {
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.userDetailsService = userDetailsService;
  }

  public AuthResponse login(AuthRequest request) {
    authenticationManager.authenticate(
      new UsernamePasswordAuthenticationToken(
        request.getEmail(),
        request.getPassword()
      )
    );

    UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
    String jwtToken = jwtService.generateToken(userDetails);

    String refreshToken = UUID.randomUUID().toString();

    return new AuthResponse(jwtToken, refreshToken);
  }
}
