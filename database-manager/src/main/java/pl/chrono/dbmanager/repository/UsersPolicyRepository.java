package pl.chrono.dbmanager.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.UsersPolicy;

@Repository
public interface UsersPolicyRepository extends JpaRepository<UsersPolicy, Integer> {
  Optional<UsersPolicy> findByUser_UserIdAndValidToIsNull(Integer userId);

  default Optional<UsersPolicy> findActiveByUserId(Integer userId) {
    return findByUser_UserIdAndValidToIsNull(userId);
  }
}
