package pl.chrono.dbmanager.repository;


import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.chrono.dbmanager.model.WorkLogRequest;
import pl.chrono.dbmanager.model.enums.WorkLogRequestStatus;

public interface WorkLogRequestRepository extends JpaRepository<WorkLogRequest, Long> {
  List<WorkLogRequest> findAllByEmployeeUserIdOrderByCreatedAtDesc(Integer employeeId);

  List<WorkLogRequest> findAllByEmployeeManagerUserIdAndStatus(Integer managerId,
                                                               WorkLogRequestStatus status);

  List<WorkLogRequest> findAllByStatus(WorkLogRequestStatus status);
}
