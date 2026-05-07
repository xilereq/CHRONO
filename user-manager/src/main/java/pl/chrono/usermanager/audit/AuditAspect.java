package pl.chrono.usermanager.audit;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import pl.chrono.dbmanager.model.OperationLog;
import pl.chrono.dbmanager.repository.OperationLogRepository;

@Aspect
@Component
@Slf4j
public class AuditAspect {
  private final OperationLogRepository logRepository;

  public AuditAspect(OperationLogRepository logRepository) {
    this.logRepository = logRepository;
  }

  @AfterReturning("@annotation(logActivity)")
  public void logOperation(JoinPoint joinPoint, LogActivity logActivity) {
    OperationLog auditLog = new OperationLog();

    auditLog.setAction(logActivity.action());
    auditLog.setDetails(logActivity.details());

    String currentUser = "SYSTEM";
    if (SecurityContextHolder.getContext().getAuthentication() != null) {
      currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
    }
    auditLog.setUserEmail(currentUser);


    auditLog.setTimestamp(LocalDateTime.now());

    logRepository.save(auditLog);
    log.info("AUDIT LOG: User {} performed {}", currentUser, logActivity.action());
  }
}
