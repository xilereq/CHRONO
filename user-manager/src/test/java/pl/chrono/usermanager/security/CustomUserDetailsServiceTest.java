package pl.chrono.usermanager.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.ports.I_UserData;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

  @Mock
  private I_UserData userData;

  @InjectMocks
  private CustomUserDetailsService customUserDetailsService;

  @Test
  void loadUserByUsername_ShouldReturnUserDetails_WhenUserExists() {
    String email = "test@test.pl";
    User user = new User();
    user.setEmail(email);
    user.setPasswordHash("hashed_password");

    Role role = new Role();
    role.setName("ROLE_ADMIN");
    user.getRoles().add(role);

    when(userData.findByEmail(email)).thenReturn(Optional.of(user));

    UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

    assertNotNull(userDetails);
    assertEquals(email, userDetails.getUsername());
    assertEquals("hashed_password", userDetails.getPassword());
    assertTrue(userDetails.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
  }

  @Test
  void loadUserByUsername_ShouldThrowException_WhenUserDoesNotExist() {
    String email = "nonexistent@test.pl";
    when(userData.findByEmail(email)).thenReturn(Optional.empty());

    UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
      customUserDetailsService.loadUserByUsername(email);
    });

    assertEquals("Nie znaleziono użytkownika: " + email, exception.getMessage());
  }
}
