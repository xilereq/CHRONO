package pl.chrono.dbmanager.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.ports.I_UserData;
import pl.chrono.dbmanager.repository.UserRepository;
import pl.chrono.dbmanager.repository.RoleRepository;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserDataAdapter implements I_UserData {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;

  @Override
  public List<User> getSubordinatesForManager(Integer managerId){
    return userRepository.findAllByManager_UserId(managerId);
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
  public User save(User user) {
    return userRepository.save(user);
  }

  @Override
  public Optional<Role> findRoleByName(String roleName) {
    return roleRepository.findByName(roleName);
  }

}
