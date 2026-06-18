package pl.chrono.dbmanager.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
  List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Integer userId);
}
