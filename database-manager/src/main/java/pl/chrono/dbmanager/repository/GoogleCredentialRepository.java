package pl.chrono.dbmanager.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.GoogleCredential;

@Repository
public interface GoogleCredentialRepository extends JpaRepository<GoogleCredential, Long> {

  Optional<GoogleCredential> findByUserUserId(Integer userId);
}
