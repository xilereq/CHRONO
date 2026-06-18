package pl.chrono.notificationmanager.listener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.applicationmanager.event.ApplicationNotificationEvent;
import pl.chrono.dbmanager.model.WorkLogRequest;
import pl.chrono.dbmanager.repository.WorkLogRequestRepository;
import pl.chrono.notificationmanager.service.NotificationService;
import pl.chrono.timemanager.event.WorkLogRequestNotificationEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

  @Mock
  private NotificationService notificationService;

  @Mock
  private WorkLogRequestRepository workLogRequestRepository;

  @InjectMocks
  private NotificationListener notificationListener;

  @Test
  void handleApplicationNotification_ShouldSendToManager_WhenCreated() {
    ApplicationNotificationEvent event = mock(ApplicationNotificationEvent.class);
    when(event.action()).thenReturn("CREATED");
    when(event.managerId()).thenReturn(2);
    when(event.applicationId()).thenReturn(100L);

    notificationListener.handleApplicationNotification(event);

    verify(notificationService, times(1)).sendNotification(
        eq(2),
        argThat(msg -> msg.contains("100")), // Sprawdza czy wiadomość zawiera ID wniosku
        eq(100L),
        eq("APPLICATION"),
        eq("APPLICATION_CREATED")
    );
  }

  @Test
  void handleApplicationNotification_ShouldDoNothing_WhenCreatedButNoManager() {
    ApplicationNotificationEvent event = mock(ApplicationNotificationEvent.class);
    when(event.action()).thenReturn("CREATED");
    when(event.managerId()).thenReturn(null);

    notificationListener.handleApplicationNotification(event);

    verify(notificationService, never()).sendNotification(any(), anyString(), anyLong(), anyString(), anyString());
  }

  @Test
  void handleApplicationNotification_ShouldSendToEmployee_WhenStatusChanged() {
    ApplicationNotificationEvent event = mock(ApplicationNotificationEvent.class);
    when(event.action()).thenReturn("STATUS_CHANGED");
    when(event.employeeId()).thenReturn(1);
    when(event.applicationId()).thenReturn(100L);
    when(event.newStatus()).thenReturn("ZAAKCEPTOWANY");
    when(event.reviewerName()).thenReturn("Jan Kowalski");

    notificationListener.handleApplicationNotification(event);

    verify(notificationService, times(1)).sendNotification(
        eq(1),
        argThat(msg -> msg.contains("ZAAKCEPTOWANY") && msg.contains("Jan Kowalski")),
        eq(100L),
        eq("APPLICATION"),
        eq("APPLICATION_STATUS_CHANGED")
    );
  }

  @Test
  void handleApplicationNotification_ShouldDoNothing_WhenActionIsUnknown() {
    ApplicationNotificationEvent event = mock(ApplicationNotificationEvent.class);
    when(event.action()).thenReturn("UNKNOWN_ACTION");

    notificationListener.handleApplicationNotification(event);

    verify(notificationService, never()).sendNotification(any(), anyString(), anyLong(), anyString(), anyString());
  }

  @Test
  void handleWorkLogRequestNotification_ShouldSendToManager_WhenCreated() {
    LocalDate fixedDate = LocalDate.of(2026, 1, 1);
    WorkLogRequestNotificationEvent event = new WorkLogRequestNotificationEvent(
        200L, 1, 3, "CREATED", "PENDING", null
    );

    WorkLogRequest mockRequest = new WorkLogRequest();
    mockRequest.setProposedStartTime(fixedDate.atStartOfDay());
    when(workLogRequestRepository.findById(200L)).thenReturn(Optional.of(mockRequest));

    notificationListener.handleWorkLogRequestNotification(event);

    verify(notificationService, times(1)).sendNotification(
        eq(3),
        argThat(msg ->  msg.contains("2026-01-01")),
        eq(200L),
        eq("WORK_LOG_REQUEST"),
        eq("WORK_LOG_REQUEST_CREATED")
    );
  }

  @Test
  void handleWorkLogRequestNotification_ShouldDoNothing_WhenCreatedButNoManager() {
    WorkLogRequestNotificationEvent event = new WorkLogRequestNotificationEvent(
        200L, 1, null, "CREATED", "PENDING", null
    );

    WorkLogRequest mockRequest = new WorkLogRequest();
    mockRequest.setProposedStartTime(LocalDateTime.now());
    when(workLogRequestRepository.findById(200L)).thenReturn(Optional.of(mockRequest));

    notificationListener.handleWorkLogRequestNotification(event);

    verify(notificationService, never()).sendNotification(any(), anyString(), anyLong(), anyString(), anyString());
  }

  @Test
  void handleWorkLogRequestNotification_ShouldSendToEmployee_WhenStatusChanged() {
    WorkLogRequestNotificationEvent event = mock(WorkLogRequestNotificationEvent.class);
    when(event.action()).thenReturn("STATUS_CHANGED");
    when(event.employeeId()).thenReturn(1);
    when(event.requestId()).thenReturn(200L);
    when(event.status()).thenReturn("APPROVED");
    when(event.reviewerName()).thenReturn("Anna Nowak");

    WorkLogRequest mockRequest = new WorkLogRequest();
    mockRequest.setProposedStartTime(LocalDateTime.now());
    when(workLogRequestRepository.findById(200L)).thenReturn(Optional.of(mockRequest));

    notificationListener.handleWorkLogRequestNotification(event);

    verify(notificationService, times(1)).sendNotification(
        eq(1),
        argThat(msg -> msg.contains("APPROVED") && msg.contains("Anna Nowak")),
        eq(200L),
        eq("WORK_LOG_REQUEST"),
        eq("WORK_LOG_REQUEST_STATUS_CHANGED")
    );
  }

  @Test
  void handleWorkLogRequestNotification_ShouldSendToEmployeeWithSystemReviewer_WhenStatusChangedAndReviewerNull() {
    WorkLogRequestNotificationEvent event = mock(WorkLogRequestNotificationEvent.class);
    when(event.action()).thenReturn("STATUS_CHANGED");
    when(event.employeeId()).thenReturn(1);
    when(event.requestId()).thenReturn(200L);
    when(event.status()).thenReturn("REJECTED");
    when(event.reviewerName()).thenReturn(null);

    WorkLogRequest mockRequest = new WorkLogRequest();
    mockRequest.setProposedStartTime(LocalDateTime.now());
    when(workLogRequestRepository.findById(200L)).thenReturn(Optional.of(mockRequest));

    notificationListener.handleWorkLogRequestNotification(event);

    verify(notificationService, times(1)).sendNotification(
        eq(1),
        argThat(msg -> msg.contains("REJECTED") && msg.contains("System")),
        eq(200L),
        eq("WORK_LOG_REQUEST"),
        eq("WORK_LOG_REQUEST_STATUS_CHANGED")
    );
  }

  @Test
  void handleWorkLogRequestNotification_ShouldDoNothing_WhenActionIsUnknown() {
    WorkLogRequestNotificationEvent event = new WorkLogRequestNotificationEvent(
        200L, 1, null, "UNKNOWN", "PENDING", null
    );

    WorkLogRequest mockRequest = new WorkLogRequest();
    mockRequest.setProposedStartTime(LocalDateTime.now());
    when(workLogRequestRepository.findById(200L)).thenReturn(Optional.of(mockRequest));

    notificationListener.handleWorkLogRequestNotification(event);

    verify(notificationService, never()).sendNotification(any(), anyString(), anyLong(), anyString(), anyString());
  }
}
