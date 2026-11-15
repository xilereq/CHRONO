package pl.chrono.dbmanager.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.chrono.dbmanager.model.GeneratedReport;
import pl.chrono.dbmanager.ports.I_GeneratedReportData;
import pl.chrono.dbmanager.repository.GeneratedReportRepository;

@Component
@RequiredArgsConstructor
public class GeneratedReportDataAdapter implements I_GeneratedReportData {

  private final GeneratedReportRepository generatedReportRepository;

  @Override
  public GeneratedReport saveGeneratedReport(GeneratedReport generatedReport) {
    return generatedReportRepository.save(generatedReport);
  }
}
