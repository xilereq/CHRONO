package pl.chrono.usermanager.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.repository.RoleRepository;
import pl.chrono.dbmanager.repository.UserRepository;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  public DataInitializer(UserRepository userRepository,
                         RoleRepository roleRepository,
                         PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional // Zapewnia poprawny zapis powiązań @ManyToMany
  public void run(String... args) throws Exception {
    // 1. Inicjalizacja Ról
    Role roleEmployee = createRoleIfNotFound("ROLE_EMPLOYEE");
    Role roleManager = createRoleIfNotFound("ROLE_MANAGER");
    Role roleHr = createRoleIfNotFound("ROLE_HR");
    Role roleAdmin = createRoleIfNotFound("ROLE_ADMIN");

    // 2. Dodawanie przykładowych użytkowników (jeśli baza jest pusta)
    if (userRepository.count() == 0) {

      // Przykład 1: Administrator
      User admin = new User();
      admin.setEmail("admin@chrono.pl");
      admin.setPasswordHash(passwordEncoder.encode("admin123")); // Szyfrowanie hasła
      admin.setRoles(Set.of(roleAdmin, roleEmployee)); // Administrator może mieć wiele ról
      userRepository.save(admin);

      // Przykład 2: Pracownik HR
      User hrUser = new User();
      hrUser.setEmail("hr@chrono.pl");
      hrUser.setPasswordHash(passwordEncoder.encode("hr123"));
      hrUser.setRoles(Set.of(roleHr, roleEmployee));
      userRepository.save(hrUser);

      // Przykład 3: Kierownik
      User manager = new User();
      manager.setEmail("kierownik@chrono.pl");
      manager.setPasswordHash(passwordEncoder.encode("manager123"));
      manager.setRoles(Set.of(roleManager, roleEmployee));
      userRepository.save(manager);

      // Przykład 4: Zwykły Pracownik przypisany do kierownika
      User employee = new User();
      employee.setEmail("pracownik@chrono.pl");
      employee.setPasswordHash(passwordEncoder.encode("user123"));
      employee.setRoles(Set.of(roleEmployee));
      employee.setManager(manager); // Przypisanie przełożonego
      employee.setOvertimeBalance(0); // Inicjalizacja salda nadgodzin
      userRepository.save(employee);
    }
  }

  private Role createRoleIfNotFound(String roleName) {
    return roleRepository.findByName(roleName) // Wymaga dodania metody findByName w RoleRepository
      .orElseGet(() -> {
        Role newRole = new Role();
        newRole.setName(roleName);
        return roleRepository.save(newRole);
      });
  }
}
