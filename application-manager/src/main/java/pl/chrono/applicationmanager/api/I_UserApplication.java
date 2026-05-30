package pl.chrono.applicationmanager.api;

import java.util.List;
import pl.chrono.applicationmanager.dto.request.CreateApplicationRequest;
import pl.chrono.applicationmanager.dto.response.ApplicationResponse;

public interface I_UserApplication {
  ApplicationResponse createApplication(CreateApplicationRequest request, Integer employeeId);

  ApplicationResponse changeStatus(Long applicationId, String newStatus, Integer reviewerId, String decisionReason);

  List<ApplicationResponse> getUserApplications(Integer employeeId, Integer requesterId);

  List<ApplicationResponse> getPendingApplicationsForManager(Integer managerId);

  List<ApplicationResponse> getAllApplicationsForManager(Integer managerId);

  List<ApplicationResponse> getAllApplications();
}
