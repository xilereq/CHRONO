package pl.chrono.dbmanager.adapter;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.GoogleCredential;
import pl.chrono.dbmanager.model.enums.ApplicationType;
import pl.chrono.dbmanager.ports.I_GoogleCalendarData;
import pl.chrono.dbmanager.repository.ApplicationRepository;
import pl.chrono.dbmanager.repository.GoogleCredentialRepository;

@Component
@RequiredArgsConstructor
public class GoogleCalendarDataAdapter implements I_GoogleCalendarData {

  private static final List<ApplicationType> SYNCABLE_TYPES = List.of(
      ApplicationType.HOLIDAY_LEAVE,
      ApplicationType.HOME_OFFICE
  );

  private final GoogleCredentialRepository googleCredentialRepository;
  private final ApplicationRepository applicationRepository;

  @Override
  public Optional<GoogleCredential> findCredentialByUserId(Integer userId) {
    return googleCredentialRepository.findByUserUserId(userId);
  }

  @Override
  public GoogleCredential saveCredential(GoogleCredential credential) {
    return googleCredentialRepository.save(credential);
  }

  @Override
  public Optional<Application> findApplicationById(Long applicationId) {
    return applicationRepository.findById(applicationId);
  }

  @Override
  public List<Application> findSyncableApplications(Integer employeeId, LocalDate fromDate) {
    return applicationRepository.findSyncableApplications(employeeId, fromDate, SYNCABLE_TYPES);
  }
}
