package pl.chrono.reportmanager.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import pl.chrono.reportmanager.dto.*;
import pl.chrono.reportmanager.generator.*;
import pl.chrono.reportmanager.renderer.ReportExporter;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class ReportFacadeIT {

  @Autowired
  private ReportFacade reportFacade;

  @MockitoBean private TimesheetReportService timesheetService;
  @MockitoBean private TeamWorkloadReportService workloadService;
  @MockitoBean private TeamAbsenceReportService absenceService;
  @MockitoBean private OvertimeReportService overtimeService;
  @MockitoBean private TeamAnomalyReportService anomalyService;
  @MockitoBean private TimesheetReportGenerator timesheetGenerator;
  @MockitoBean private WorkloadReportGenerator workloadGenerator;
  @MockitoBean private AbsenceReportGenerator absenceGenerator;
  @MockitoBean private EmployeeOvertimeReportGenerator employeeOvertimeGenerator;
  @MockitoBean private TeamOvertimeReportGenerator teamOvertimeGenerator;
  @MockitoBean private AnomalyReportGenerator anomalyGenerator;
  @MockitoBean private ReportExporter exporter;

  @Test
  void shouldOrchestrateEmployeeTimesheetGeneration() {
    Integer employeeId = 1;
    LocalDate start = LocalDate.now();
    LocalDate end = LocalDate.now();
    ReportFormat format = ReportFormat.PDF;
    byte[] expectedBytes = {1, 2, 3};

    EmployeeTimesheetReport mockReport = new EmployeeTimesheetReport(employeeId, "Test", start, end, 0L, 0L, 0L, 0L, null);

    when(timesheetService.generateTimesheet(employeeId, start, end)).thenReturn(mockReport);
    ReportDocument document = new ReportDocument("Raport", List.of(), List.of(), List.of());
    when(timesheetGenerator.generate(any())).thenReturn(document);
    when(exporter.export(document, format)).thenReturn(expectedBytes);

    byte[] result = reportFacade.getEmployeeTimesheet(employeeId, start, end, format);

    assertThat(result).isEqualTo(expectedBytes);
    verify(timesheetService).generateTimesheet(employeeId, start, end);
    verify(timesheetGenerator).generate(any());
    verify(exporter).export(document, format);
  }

  @Test
  void shouldOrchestrateTeamWorkloadGeneration() {
    Integer managerId = 1;
    LocalDate start = LocalDate.now();
    LocalDate end = LocalDate.now();
    ReportFormat format = ReportFormat.CSV;
    byte[] expectedBytes = {4, 5, 6};

    TeamWorkloadReport mockReport = new TeamWorkloadReport(managerId, start, end, null);

    when(workloadService.generateReport(managerId, start, end)).thenReturn(mockReport);
    ReportDocument document = new ReportDocument("Raport", List.of(), List.of(), List.of());
    when(workloadGenerator.generate(any())).thenReturn(document);
    when(exporter.export(document, format)).thenReturn(expectedBytes);

    byte[] result = reportFacade.getTeamWorkload(managerId, start, end, format);

    assertThat(result).isEqualTo(expectedBytes);
    verify(workloadService).generateReport(managerId, start, end);
    verify(workloadGenerator).generate(any());
    verify(exporter).export(document, format);
  }
}
