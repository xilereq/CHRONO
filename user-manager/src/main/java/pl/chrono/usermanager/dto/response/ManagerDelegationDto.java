package pl.chrono.usermanager.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ManagerDelegationDto(
    Long id,
    Integer managerId,
    String managerName,
    Integer deputyId,
    String deputyName,
    LocalDate startDate,
    LocalDate endDate,
    String reason,
    String status,
    Integer createdById,
    LocalDateTime createdAt,
    LocalDateTime revokedAt
) {}
