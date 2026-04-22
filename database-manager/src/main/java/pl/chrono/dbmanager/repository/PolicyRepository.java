package pl.chrono.dbmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.Policy;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Integer> {
}
