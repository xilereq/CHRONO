package pl.chrono.dbmanager.ports;

import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;
import java.util.List;
import java.util.Optional;

public interface I_UserData {

  List<User> getSubordinatesForManager(Integer managerId);

  Optional<User> findByEmail(String email);

  Optional<User> findByResetToken(String resetToken);
  User save(User user);

  Optional<Role> findRoleByName(String roleName);

}
