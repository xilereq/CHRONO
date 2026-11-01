package pl.chrono.usermanager.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pl.chrono.dbmanager.model.User;
import pl.chrono.usermanager.api.I_ManagerDelegationService;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String ROLE_MANAGER = "ROLE_MANAGER";

  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;
  private final I_ManagerDelegationService delegationService;

  public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService,
                                 I_ManagerDelegationService delegationService) {
    this.jwtService = jwtService;
    this.userDetailsService = userDetailsService;
    this.delegationService = delegationService;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain
  ) throws ServletException, IOException {

    if (SecurityContextHolder.getContext().getAuthentication() != null) {
      filterChain.doFilter(request, response);
      return;
    }

    final String authHeader = request.getHeader("Authorization");
    final String jwt;
    final String userEmail;

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    jwt = authHeader.substring(7);
    userEmail = jwtService.extractUsername(jwt);

    if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

      if (jwtService.isTokenValid(jwt, userDetails)) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            resolveAuthorities(userDetails)
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
      }
    }
    filterChain.doFilter(request, response);
  }

  Collection<? extends GrantedAuthority> resolveAuthorities(UserDetails userDetails) {
    Collection<? extends GrantedAuthority> own = userDetails.getAuthorities();
    if (!(userDetails instanceof User user)) {
      return own;
    }
    boolean alreadyManager = own.stream().anyMatch(a -> ROLE_MANAGER.equals(a.getAuthority()));
    if (alreadyManager || !delegationService.isActingDeputy(user.getUserId())) {
      return own;
    }
    List<GrantedAuthority> extended = new ArrayList<>(own);
    extended.add(new SimpleGrantedAuthority(ROLE_MANAGER));
    return extended;
  }
}
