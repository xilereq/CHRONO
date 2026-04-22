package pl.chrono.dbmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.UsersPolicy;

@Repository
public interface UsersPolicyRepository extends JpaRepository<UsersPolicy, Integer> {
}
