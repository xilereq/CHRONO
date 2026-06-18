package pl.chrono.timemanager.dto.response;

import java.time.LocalDateTime;

public record OvertimeRecordDto(
    Long id,
    String type,
    Integer durationSeconds,
    LocalDateTime createdAt,
    String note
) {
}
