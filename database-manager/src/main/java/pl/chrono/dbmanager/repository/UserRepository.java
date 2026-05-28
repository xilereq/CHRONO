package pl.chrono.dbmanager.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
  Optional<User> findByEmail(String email);
  Optional<User> findByResetToken(String resetToken);
  Optional<User> findByName(String name);
  Optional<User> findBySurname(String surname);

  List<User> findAllByManager_UserId(Integer managerId);
  List<User> findByRoles_Name(String role);
  List<User> findAllByIsActiveTrue();
  List<User> findByIsActiveFalseAndEmploymentEndDateBefore(LocalDate cutoffDate);
}
