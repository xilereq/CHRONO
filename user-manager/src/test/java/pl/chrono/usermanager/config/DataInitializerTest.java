package pl.chrono.usermanager.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.repository.RoleRepository;
import pl.chrono.dbmanager.repository.UserRepository;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

  @Mock private UserRepository userRepository;
  @Mock private RoleRepository roleRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JdbcTemplate jdbcTemplate;

  @InjectMocks
  private DataInitializer dataInitializer;

  @Test
  void run_ShouldInitializeRolesAndUsers_WhenDatabaseIsEmpty() throws Exception {
    when(userRepository.count()).thenReturn(0L);
    when(roleRepository.findByName(anyString())).thenReturn(Optional.empty());

    when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");

    dataInitializer.run();

    verify(jdbcTemplate, times(1))
        .execute("UPDATE users SET force_password_change = false WHERE force_password_change IS NULL");

    verify(roleRepository, times(4)).findByName(anyString());
    verify(roleRepository, times(4)).save(any(Role.class));

    verify(userRepository, times(4)).save(any(User.class));
  }

  @Test
  void run_ShouldNotCreateUsers_WhenDatabaseIsNotEmptyAndRolesExist() throws Exception {
    when(userRepository.count()).thenReturn(5L);

    when(roleRepository.findByName(anyString())).thenAnswer(invocation -> {
      Role role = new Role();
      role.setName(invocation.getArgument(0));
      return Optional.of(role);
    });

    dataInitializer.run();

    verify(jdbcTemplate, times(1))
        .execute("UPDATE users SET force_password_change = false WHERE force_password_change IS NULL");

    verify(roleRepository, times(4)).findByName(anyString());
    verify(roleRepository, never()).save(any(Role.class));

    verify(userRepository, never()).save(any(User.class));
  }
}
