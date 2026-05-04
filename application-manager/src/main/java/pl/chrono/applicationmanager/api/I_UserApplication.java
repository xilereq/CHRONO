package pl.chrono.applicationmanager.api;

import java.util.List;
import pl.chrono.applicationmanager.dto.ApplicationDTO;

public interface I_UserApplication {
  ApplicationDTO createApplication(ApplicationDTO request, Integer employeeId);

  ApplicationDTO changeStatus(Long applicationId, String newStatus, Integer reviewerId);

  List<ApplicationDTO> getUserApplications(Integer employeeId);
}
