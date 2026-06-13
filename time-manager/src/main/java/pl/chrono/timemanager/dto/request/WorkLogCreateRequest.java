package pl.chrono.timemanager.dto.request;

import pl.chrono.dbmanager.model.enums.WorkLogRequestType;
import java.time.LocalDateTime;

public record WorkLogCreateRequest(
    WorkLogRequestType type,
    Integer targetWorkLogId,
    LocalDateTime proposedStartTime,
    LocalDateTime proposedEndTime,
    String reason
) {}

