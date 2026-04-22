package pl.chrono.dbmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
}
