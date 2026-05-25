package pl.chrono.usermanager.config;

import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.repository.RoleRepository;
import pl.chrono.dbmanager.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final JdbcTemplate jdbcTemplate;

  public DataInitializer(UserRepository userRepository,
                         RoleRepository roleRepository,
                         PasswordEncoder passwordEncoder,
                         JdbcTemplate jdbcTemplate) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  @Transactional
  public void run(String... args) throws Exception {

    jdbcTemplate.execute("UPDATE users SET force_password_change = false WHERE force_password_change IS NULL");
    Role roleEmployee = createRoleIfNotFound("ROLE_EMPLOYEE");
    Role roleManager = createRoleIfNotFound("ROLE_MANAGER");
    Role roleHr = createRoleIfNotFound("ROLE_HR");
    Role roleAdmin = createRoleIfNotFound("ROLE_ADMIN");

    if (userRepository.count() == 0) {

      User admin = new User();
      admin.setEmail("admin@chrono.pl");
      admin.setName("test");
      admin.setSurname("test");
      admin.setPasswordHash(passwordEncoder.encode("admin123"));
      admin.setRoles(Set.of(roleAdmin));
      admin.setForcePasswordChange(false);
      userRepository.save(admin);

      User hrUser = new User();
      hrUser.setEmail("hr@chrono.pl");
      hrUser.setName("test");
      hrUser.setSurname("test");
      hrUser.setPasswordHash(passwordEncoder.encode("hr123"));
      hrUser.setRoles(Set.of(roleHr));
      hrUser.setForcePasswordChange(false);
      userRepository.save(hrUser);

      User manager = new User();
      manager.setEmail("kierownik@chrono.pl");
      manager.setName("test");
      manager.setSurname("test");
      manager.setPasswordHash(passwordEncoder.encode("manager123"));
      manager.setRoles(Set.of(roleManager));
      manager.setForcePasswordChange(false);
      userRepository.save(manager);

      User employee = new User();
      employee.setEmail("pracownik@chrono.pl");
      employee.setName("test");
      employee.setSurname("test");
      employee.setPasswordHash(passwordEncoder.encode("user123"));
      employee.setRoles(Set.of(roleEmployee));
      employee.setManager(manager);
      employee.setOvertimeBalance(0);
      employee.setForcePasswordChange(false);
      userRepository.save(employee);
    }
  }

  private Role createRoleIfNotFound(String roleName) {
    return roleRepository.findByName(roleName)
        .orElseGet(() -> {
          Role newRole = new Role();
          newRole.setName(roleName);
          return roleRepository.save(newRole);
        });
  }
}
