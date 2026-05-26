package pl.chrono.logmanager.audit;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import pl.chrono.dbmanager.model.OperationLog;
import pl.chrono.dbmanager.repository.OperationLogRepository;

@Aspect
@Component
@Slf4j
public class AuditAspect {
  private final OperationLogRepository logRepository;
  private final ExpressionParser parser = new SpelExpressionParser();

  public AuditAspect(OperationLogRepository logRepository) {
    this.logRepository = logRepository;
  }

  @AfterReturning("@annotation(logActivity)")
  public void logOperation(JoinPoint joinPoint, LogActivity logActivity) {
    OperationLog auditLog = new OperationLog();

    auditLog.setAction(logActivity.action());
    String details = logActivity.details();

    if(details.contains("#")){
      MethodSignature signature = (MethodSignature) joinPoint.getSignature();
      String[] paramNames = signature.getParameterNames();
      Object[] args = joinPoint.getArgs();

      StandardEvaluationContext context = new StandardEvaluationContext();
      if(paramNames != null){
        for (int i = 0; i < args.length; i++){
          context.setVariable(paramNames[i], args[i]);
        }
      }

      try {
        details = parser.parseExpression(details).getValue(context, String.class);
      } catch (Exception e) {
        log.warn("Nie udało się przeparsować wyrażenia SpEL w logu: {}", details);
      }
    }

    auditLog.setDetails(details);

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
