package pl.chrono.logmanager.dto;

import java.time.LocalDateTime;

public record OperationLogDto(
   long logID,
   String action,
   String details,
   LocalDateTime timestamp,
   String userEmail
) {}
