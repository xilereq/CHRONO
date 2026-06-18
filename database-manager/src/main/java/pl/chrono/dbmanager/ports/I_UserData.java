package pl.chrono.dbmanager.ports;

import java.util.List;
import java.util.Optional;
import pl.chrono.dbmanager.model.Role;
import pl.chrono.dbmanager.model.User;

public interface I_UserData {

  List<User> getSubordinatesForManager(Integer managerId);

  List<User> findByRoles_Name(String role);

  List<User> findAll();

  Optional<User> findByEmail(String email);

  Optional<User> findByName(String name);

  Optional<User> findBySurname(String surname);

  Optional<User> findByResetToken(String resetToken);

  User save(User user);

  Optional<Role> findRoleByName(String roleName);

}
