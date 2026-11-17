package pl.chrono.timemanager.dto.response;

import java.time.LocalDateTime;
import pl.chrono.dbmanager.model.enums.ApplicationType;

public record TeamMemberStatusDto(
    Integer userId,
    String fullName,
    TeamPresenceStatus status,
    boolean currentlyWorking,
    LocalDateTime firstStartTime,
    ApplicationType leaveType
) {
}
