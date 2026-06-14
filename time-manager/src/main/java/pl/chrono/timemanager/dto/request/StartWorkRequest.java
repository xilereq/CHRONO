package pl.chrono.timemanager.dto.request;

import pl.chrono.dbmanager.model.enums.WorkLocationType;

public record StartWorkRequest(WorkLocationType locationType) {
}
