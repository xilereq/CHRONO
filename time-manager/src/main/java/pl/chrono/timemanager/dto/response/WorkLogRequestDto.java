package pl.chrono.timemanager.dto.response;

import java.time.LocalDateTime;
import pl.chrono.dbmanager.model.enums.WorkLogRequestStatus;
import pl.chrono.dbmanager.model.enums.WorkLogRequestType;

public record WorkLogRequestDto(
    Long id,
    Integer employeeId,
    String employeeName,
    Integer managerId,
    String managerName,
    Integer targetWorkLogId,
    WorkLogRequestType type,
    WorkLogRequestStatus status,
    LocalDateTime proposedStartTime,
    LocalDateTime proposedEndTime,
    String reason,
    String managerComment,
    LocalDateTime createdAt,
    LocalDateTime resolvedAt
) {}
