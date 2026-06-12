package pl.chrono.logmanager.audit;

import java.time.LocalDateTime;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pl.chrono.dbmanager.model.OperationLog;
import pl.chrono.dbmanager.repository.OperationLogRepository;

@Component
public class LoginSuccessListener implements ApplicationListener<AuthenticationSuccessEvent> {
  private final OperationLogRepository logRepository;
  private final HttpServletRequest request;

  public LoginSuccessListener(OperationLogRepository logRepository, HttpServletRequest request) {
    this.logRepository = logRepository;
    this.request = request;
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

    String deviceType = "UNKNOWN";

    try {
      ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes != null) {
        String userAgent = (String) attributes.getAttribute("Saved-User-Agent", RequestAttributes.SCOPE_REQUEST);

        if (userAgent == null) {
          userAgent = attributes.getRequest().getHeader("User-Agent");
        }

        if (userAgent != null && !userAgent.isEmpty()) {
          String uaLower = userAgent.toLowerCase();
          if (uaLower.contains("mobi") || uaLower.contains("android") || uaLower.contains("iphone") || uaLower.contains("ipad")) {
            deviceType = "MOBILE";
          } else {
            deviceType = "COMPUTER";
          }
        } else {
          deviceType = "NO_HEADER";
        }
      } else {
        deviceType = "NO_CONTEXT";
      }
    } catch (Exception e) {
      deviceType = "ERROR";
      System.err.println("Błąd odczytu User-Agent: " + e.getMessage());
    }

    OperationLog auditLog = new OperationLog();
    auditLog.setAction("USER_LOGIN");
    auditLog.setUserEmail(userEmail);
    auditLog.setDetails("Successful login (Device: " + deviceType + ")");
    auditLog.setTimestamp(LocalDateTime.now());

    logRepository.save(auditLog);
  }
}
