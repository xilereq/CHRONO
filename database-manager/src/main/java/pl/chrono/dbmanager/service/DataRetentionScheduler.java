package pl.chrono.dbmanager.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pl.chrono.dbmanager.model.User;
import pl.chrono.dbmanager.repository.OperationLogRepository;
import pl.chrono.dbmanager.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Component
@RequiredArgsConstructor
@Slf4j
public class DataRetentionScheduler {

  private final OperationLogRepository operationLogRepository;
  private final UserRepository userRepository;
  @Value("${app.retention.user-years:10}")
  private int userRetentionYears;

  @Value("${app.retention.logs-months:60}")
  private int operationLogRetentionMonths;

  @Scheduled(cron = "0 0 2 * * ?")
  @Transactional
  public void cleanupOperationLogs() {
    log.info("Rozpoczęto usuwanie przeterminowanych logów operacji...");
    LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(operationLogRetentionMonths);

    int deletedCount = operationLogRepository.deleteOlderThan(cutoffDate);
    log.info("Usunięto {} logów operacji starszych niż {}.", deletedCount, cutoffDate);
  }

  @Scheduled(cron = "0 0 3 ? * SUN")
  @Transactional
  public void anonymizeOldUsers() {
    log.info("Rozpoczęto anonimizację zwolnionych użytkowników...");

    LocalDate cutoffDate = LocalDate.now().minusYears(userRetentionYears);

    List<User> usersToAnonymize = userRepository.findByIsActiveFalseAndEmploymentEndDateBefore(cutoffDate);

    if(usersToAnonymize.isEmpty()){
      log.info("Brak użytkowników do anonimizacji w tym miesiącu.");
      return;
    }

    for(User user : usersToAnonymize){
      anonymizeUserData(user);
    }

    userRepository.saveAll(usersToAnonymize);

    log.info("Pomyślnie zanonimizowano {} użytkowników, których zwolniono przed {}.",
        usersToAnonymize.size(), cutoffDate);
  }

  private void anonymizeUserData(User user){
    user.setName("Zanonimizowano");
    user.setSurname("Zanonimizowano");

    user.setEmail("anon_" + UUID.randomUUID() + "@deleted.chrono.pl");

    user.setPasswordHash("DELETED");
    user.setRefreshToken(null);
    user.setRefreshExpiration(null);
    user.setResetToken(null);
    user.setResetTokenExpiration(null);

    user.setManager(null);

    user.getRoles().clear();
  }
}
