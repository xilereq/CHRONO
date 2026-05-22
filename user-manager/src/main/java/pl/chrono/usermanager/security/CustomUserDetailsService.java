package pl.chrono.usermanager.security;

import java.util.stream.Collectors;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.ports.I_UserData;

@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final I_UserData userData;

  public CustomUserDetailsService(I_UserData userData) {
    this.userData = userData;
  }

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    User user = userData.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("Nie znaleziono użytkownika: " + email));

    var authorities = user.getRoles().stream()
        .map(role -> new SimpleGrantedAuthority(role.getName()))
        .collect(Collectors.toList());

    return new org.springframework.security.core.userdetails.User(
        user.getEmail(),
        user.getPasswordHash(),
        authorities
    );
  }
}
