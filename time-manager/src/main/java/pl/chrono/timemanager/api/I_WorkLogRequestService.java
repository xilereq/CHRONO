package pl.chrono.timemanager.api;

import java.util.List;
import pl.chrono.timemanager.dto.request.WorkLogCreateRequest;
import pl.chrono.timemanager.dto.request.WorkLogResolveRequest;
import pl.chrono.timemanager.dto.response.WorkLogRequestDto;

public interface I_WorkLogRequestService {
  void submitRequest(Integer employeeId, WorkLogCreateRequest requestDto);

  void approveRequest(Long requestId, Integer reviewerId, WorkLogResolveRequest resolveDto);

  void rejectRequest(Long requestId, Integer reviewerId, WorkLogResolveRequest resolveDto);

  List<WorkLogRequestDto> getEmployeeRequests(Integer employeeId);

  List<WorkLogRequestDto> getPendingRequestsForReviewer(Integer reviewerId);
}
