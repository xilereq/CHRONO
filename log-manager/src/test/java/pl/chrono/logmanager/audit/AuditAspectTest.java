package pl.chrono.logmanager.audit;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import pl.chrono.dbmanager.model.OperationLog;
import pl.chrono.dbmanager.repository.OperationLogRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

  @Mock private OperationLogRepository logRepository;
  @Mock private JoinPoint joinPoint;
  @Mock private LogActivity logActivity;
  @Mock private MethodSignature methodSignature;

  @InjectMocks
  private AuditAspect auditAspect;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void logOperation_ShouldLogSystem_WhenNoSecurityContext() {
    when(logActivity.action()).thenReturn("TEST_ACTION");
    when(logActivity.details()).thenReturn("Simple details without hash");

    auditAspect.logOperation(joinPoint, logActivity);

    ArgumentCaptor<OperationLog> logCaptor = ArgumentCaptor.forClass(OperationLog.class);
    verify(logRepository).save(logCaptor.capture());

    OperationLog savedLog = logCaptor.getValue();
    assertEquals("TEST_ACTION", savedLog.getAction());
    assertEquals("Simple details without hash", savedLog.getDetails());
    assertEquals("SYSTEM", savedLog.getUserEmail());
  }

  @Test
  void logOperation_ShouldLogCurrentUser_WhenContextExists() {
    when(logActivity.action()).thenReturn("TEST_ACTION");
    when(logActivity.details()).thenReturn("Simple details");

    SecurityContext context = mock(SecurityContext.class);
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("user@test.pl", null);
    when(context.getAuthentication()).thenReturn(auth);
    SecurityContextHolder.setContext(context);

    auditAspect.logOperation(joinPoint, logActivity);

    ArgumentCaptor<OperationLog> logCaptor = ArgumentCaptor.forClass(OperationLog.class);
    verify(logRepository).save(logCaptor.capture());
    assertEquals("user@test.pl", logCaptor.getValue().getUserEmail());
  }

  @Test
  void logOperation_ShouldParseSpelExpression_WhenDetailsContainHash() {
    when(logActivity.action()).thenReturn("DYNAMIC_ACTION");
    when(logActivity.details()).thenReturn("'Modified item ID: ' + #itemId");

    when(joinPoint.getSignature()).thenReturn(methodSignature);
    when(methodSignature.getParameterNames()).thenReturn(new String[]{"itemId"});
    when(joinPoint.getArgs()).thenReturn(new Object[]{123});

    auditAspect.logOperation(joinPoint, logActivity);

    ArgumentCaptor<OperationLog> logCaptor = ArgumentCaptor.forClass(OperationLog.class);
    verify(logRepository).save(logCaptor.capture());
    assertEquals("Modified item ID: 123", logCaptor.getValue().getDetails());
  }

  @Test
  void logOperation_ShouldHandleSpelException_WhenExpressionIsInvalid() {
    when(logActivity.action()).thenReturn("ERROR_ACTION");
    when(logActivity.details()).thenReturn("Invalid #expression {");

    when(joinPoint.getSignature()).thenReturn(methodSignature);
    when(methodSignature.getParameterNames()).thenReturn(new String[]{"expression"});
    when(joinPoint.getArgs()).thenReturn(new Object[]{"val"});

    auditAspect.logOperation(joinPoint, logActivity);

    ArgumentCaptor<OperationLog> logCaptor = ArgumentCaptor.forClass(OperationLog.class);
    verify(logRepository).save(logCaptor.capture());

    assertEquals("Invalid #expression {", logCaptor.getValue().getDetails());
  }
}
