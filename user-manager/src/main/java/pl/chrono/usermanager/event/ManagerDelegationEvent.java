package pl.chrono.usermanager.event;

import java.time.LocalDate;

public record ManagerDelegationEvent(
    Long delegationId,
    Integer managerId,
    String managerName,
    Integer deputyId,
    String deputyName,
    LocalDate startDate,
    LocalDate endDate,
    String action
) {}
