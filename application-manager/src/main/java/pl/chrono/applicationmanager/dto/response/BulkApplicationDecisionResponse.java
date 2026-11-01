package pl.chrono.applicationmanager.dto.response;

import java.util.List;

public record BulkApplicationDecisionResponse(
    List<ApplicationResponse> processed,
    List<BulkOperationFailure> failed
) {}
