package pl.chrono.logmanager.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pl.chrono.dbmanager.model.OperationLog;
import pl.chrono.dbmanager.repository.OperationLogRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginSuccessListenerTest {

  @Mock private OperationLogRepository logRepository;
  @Mock private HttpServletRequest injectedRequest;

  @Mock private AuthenticationSuccessEvent event;
  @Mock private Authentication authentication;
  @Mock private ServletRequestAttributes attributes;
  @Mock private HttpServletRequest contextRequest;

  @InjectMocks
  private LoginSuccessListener listener;

  @BeforeEach
  void setUp() {
    when(event.getAuthentication()).thenReturn(authentication);
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void onApplicationEvent_ShouldLogComputer_WhenUserDetailsAndComputerUserAgent() {
    // Arrange
    UserDetails userDetails = mock(UserDetails.class);
    when(userDetails.getUsername()).thenReturn("computer@test.pl");
    when(authentication.getPrincipal()).thenReturn(userDetails);

    RequestContextHolder.setRequestAttributes(attributes);
    when(attributes.getAttribute("Saved-User-Agent", RequestAttributes.SCOPE_REQUEST)).thenReturn(null);
    when(attributes.getRequest()).thenReturn(contextRequest);
    when(contextRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0 Windows NT 10.0");

    listener.onApplicationEvent(event);

    ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
    verify(logRepository).save(captor.capture());
    assertEquals("computer@test.pl", captor.getValue().getUserEmail());
    assertTrue(captor.getValue().getDetails().contains("Device: COMPUTER"));
  }

  @Test
  void onApplicationEvent_ShouldLogMobile_WhenStringPrincipalAndMobileUserAgent() {
    when(authentication.getPrincipal()).thenReturn("mobile@test.pl");

    RequestContextHolder.setRequestAttributes(attributes);
    when(attributes.getAttribute("Saved-User-Agent", RequestAttributes.SCOPE_REQUEST)).thenReturn("Mozilla/5.0 (iPhone; CPU iPhone OS)");

    listener.onApplicationEvent(event);

    ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
    verify(logRepository).save(captor.capture());
    assertEquals("mobile@test.pl", captor.getValue().getUserEmail());
    assertTrue(captor.getValue().getDetails().contains("Device: MOBILE"));
  }

  @Test
  void onApplicationEvent_ShouldLogUnknownUserAndNoHeader_WhenPrincipalIsOtherAndHeaderIsEmpty() {
    when(authentication.getPrincipal()).thenReturn(new Object());

    RequestContextHolder.setRequestAttributes(attributes);
    when(attributes.getAttribute("Saved-User-Agent", RequestAttributes.SCOPE_REQUEST)).thenReturn(null);
    when(attributes.getRequest()).thenReturn(contextRequest);
    when(contextRequest.getHeader("User-Agent")).thenReturn("");

    listener.onApplicationEvent(event);

    ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
    verify(logRepository).save(captor.capture());
    assertEquals("UNKNOWN", captor.getValue().getUserEmail());
    assertTrue(captor.getValue().getDetails().contains("Device: NO_HEADER"));
  }

  @Test
  void onApplicationEvent_ShouldLogNoContext_WhenRequestContextIsNull() {
    when(authentication.getPrincipal()).thenReturn("user@test.pl");

    listener.onApplicationEvent(event);

    ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
    verify(logRepository).save(captor.capture());
    assertTrue(captor.getValue().getDetails().contains("Device: NO_CONTEXT"));
  }

  @Test
  void onApplicationEvent_ShouldLogError_WhenExceptionThrown() {
    when(authentication.getPrincipal()).thenReturn("user@test.pl");
    RequestContextHolder.setRequestAttributes(attributes);

    when(attributes.getAttribute("Saved-User-Agent", RequestAttributes.SCOPE_REQUEST))
        .thenThrow(new RuntimeException("Wymuszony błąd odczytu"));

    listener.onApplicationEvent(event);

    ArgumentCaptor<OperationLog> captor = ArgumentCaptor.forClass(OperationLog.class);
    verify(logRepository).save(captor.capture());
    assertTrue(captor.getValue().getDetails().contains("Device: ERROR"));
  }
}
