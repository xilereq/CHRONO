package pl.chrono.dbmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.UsersPolicy;
import java.util.Optional;

@Repository
public interface UsersPolicyRepository extends JpaRepository<UsersPolicy, Integer> {
  Optional<UsersPolicy> findByUserIdAndValidToIsNull(Integer userId);

  default Optional<UsersPolicy> findActiveByUserId(Integer userId) {
    return findByUserIdAndValidToIsNull(userId);
  }
}
