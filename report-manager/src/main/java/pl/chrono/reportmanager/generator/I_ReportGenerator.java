package pl.chrono.reportmanager.generator;

import pl.chrono.reportmanager.dto.ReportDocument;

public interface I_ReportGenerator<T> {

  ReportDocument generate(T report);
}
