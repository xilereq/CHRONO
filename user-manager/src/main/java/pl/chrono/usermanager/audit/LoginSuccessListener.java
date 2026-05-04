package pl.chrono.usermanager.audit;

import java.time.LocalDateTime;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import pl.chrono.dbmanager.model.OperationLog;
import pl.chrono.dbmanager.repository.OperationLogRepository;

@Component
public class LoginSuccessListener implements ApplicationListener<AuthenticationSuccessEvent> {
  private final OperationLogRepository logRepository;

  public LoginSuccessListener(OperationLogRepository logRepository) {
    this.logRepository = logRepository;
  }

  @Override
  public void onApplicationEvent(AuthenticationSuccessEvent event) {
    Object principal = event.getAuthentication().getPrincipal();
    String userEmail = "UNKNOWN";

    if (principal instanceof UserDetails) {
      userEmail = ((UserDetails) principal).getUsername();
    } else if (principal instanceof String) {
      userEmail = (String) principal;
    }

    OperationLog auditLog = new OperationLog();
    auditLog.setAction("USER_LOGIN");
    auditLog.setUserEmail(userEmail);
    auditLog.setDetails("Successful login");
    auditLog.setTimestamp(LocalDateTime.now());

    logRepository.save(auditLog);
  }
}
