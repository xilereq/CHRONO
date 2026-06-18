package pl.chrono.dbmanager.adapter;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.ports.I_UserData;
import pl.chrono.dbmanager.repository.RoleRepository;
import pl.chrono.dbmanager.repository.UserRepository;

@Component
@RequiredArgsConstructor
public class UserDataAdapter implements I_UserData {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;

  @Override
  public List<User> getSubordinatesForManager(Integer managerId) {
    return userRepository.findAllByManager_UserId(managerId);
  }

  @Override
  public List<User> findByRoles_Name(String role) {
    return userRepository.findByRoles_Name(role);
  }

  @Override
  public List<User> findAll() {
    return userRepository.findAll();
  }

  @Override
  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email);
  }

  @Override
  public Optional<User> findByResetToken(String resetToken) {
    return userRepository.findByResetToken(resetToken);
  }

  @Override
  public Optional<User> findByName(String name) {
    return userRepository.findByName(name);
  }

  @Override
  public Optional<User> findBySurname(String surname) {
    return userRepository.findBySurname(surname);
  }

  @Override
  public User save(User user) {
    return userRepository.save(user);
  }

  @Override
  public Optional<Role> findRoleByName(String roleName) {
    return roleRepository.findByName(roleName);
  }


}
