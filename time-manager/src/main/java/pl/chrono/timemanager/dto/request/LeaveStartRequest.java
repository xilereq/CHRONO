package pl.chrono.timemanager.dto.request;

import pl.chrono.dbmanager.model.enums.LeaveType;

public record LeaveStartRequest(LeaveType type) {}
