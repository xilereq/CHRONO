package pl.chrono.workforceapi.dto.request;

import pl.chrono.dbmanager.model.enums.LeaveType;

public record LeaveStartRequest(LeaveType type) {}
