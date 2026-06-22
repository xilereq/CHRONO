package pl.chrono.usermanager.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pl.chrono.dbmanager.ports.I_UserData;

@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final I_UserData userData;

  public CustomUserDetailsService(I_UserData userData) {
    this.userData = userData;
  }

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    return userData.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("Nie znaleziono użytkownika: " + email));
  }
}
