package pl.chrono.dbmanager.ports;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import pl.chrono.dbmanager.model.Application;
import pl.chrono.dbmanager.model.GoogleCredential;

public interface I_GoogleCalendarData {

  Optional<GoogleCredential> findCredentialByUserId(Integer userId);

  GoogleCredential saveCredential(GoogleCredential credential);

  Optional<Application> findApplicationById(Long applicationId);

  List<Application> findSyncableApplications(Integer employeeId, LocalDate fromDate);
}
