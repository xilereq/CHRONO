package pl.chrono.applicationmanager.api;

import java.util.List;
import pl.chrono.applicationmanager.dto.request.CreateApplicationRequest;
import pl.chrono.applicationmanager.dto.response.ApplicationResponse;

public interface I_UserApplication {
  ApplicationResponse createApplication(CreateApplicationRequest request, Integer employeeId);

  ApplicationResponse createApplicationForEmployee(CreateApplicationRequest request, Integer targetEmployeeId, Integer managerId);

  ApplicationResponse changeStatus(Long applicationId, String newStatus, Integer reviewerId, String decisionReason, Boolean isAdmin);

  List<ApplicationResponse> getUserApplications(Integer employeeId, Integer requesterId);

  List<ApplicationResponse> getPendingApplicationsForManager(Integer managerId);

  List<ApplicationResponse> getAllApplicationsForManager(Integer managerId);

  List<ApplicationResponse> getAllApplications();

  ApplicationResponse cancelApplication(Long applicationId, Integer userId);
}
