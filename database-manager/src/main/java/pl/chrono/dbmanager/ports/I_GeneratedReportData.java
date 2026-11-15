package pl.chrono.dbmanager.ports;

import pl.chrono.dbmanager.model.GeneratedReport;

public interface I_GeneratedReportData {

  GeneratedReport saveGeneratedReport(GeneratedReport generatedReport);
}
