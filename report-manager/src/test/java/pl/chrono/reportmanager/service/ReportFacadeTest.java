package pl.chrono.reportmanager.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.reportmanager.dto.*;
import pl.chrono.reportmanager.generator.*;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportFacadeTest {

  @Mock private TimesheetReportService timesheetService;
  @Mock private TeamWorkloadReportService workloadService;
  @Mock private TeamAbsenceReportService absenceService;
  @Mock private TimesheetReportGenerator timesheetGenerator;
  @Mock private WorkloadReportGenerator workloadGenerator;
  @Mock private AbsenceReportGenerator absenceGenerator;
  @Mock private OvertimeReportService overtimeService;
  @Mock private EmployeeOvertimeReportGenerator employeeOvertimeGenerator;
  @Mock private TeamOvertimeReportGenerator teamOvertimeGenerator;
  @Mock private TeamAnomalyReportService anomalyService;
  @Mock private AnomalyReportGenerator anomalyGenerator;

  @InjectMocks private ReportFacade reportFacade;

  @Test
  void shouldGetEmployeeTimesheet() {
    Integer employeeId = 1;
    LocalDate start = LocalDate.now();
    LocalDate end = LocalDate.now();
    ReportFormat format = ReportFormat.PDF;
    byte[] expectedBytes = {1, 2, 3};
    EmployeeTimesheetReport reportData = mock(EmployeeTimesheetReport.class);

    when(timesheetService.generateTimesheet(employeeId, start, end)).thenReturn(reportData);
    when(timesheetGenerator.generate(reportData, format)).thenReturn(expectedBytes);

    byte[] result = reportFacade.getEmployeeTimesheet(employeeId, start, end, format);

    assertThat(result).isEqualTo(expectedBytes);
    verify(timesheetService).generateTimesheet(employeeId, start, end);
    verify(timesheetGenerator).generate(reportData, format);
  }

  @Test
  void shouldGetTeamWorkload() {
    Integer managerId = 1;
    LocalDate start = LocalDate.now();
    LocalDate end = LocalDate.now();
    ReportFormat format = ReportFormat.CSV;
    byte[] expectedBytes = {4, 5, 6};
    TeamWorkloadReport reportData = mock(TeamWorkloadReport.class);

    when(workloadService.generateReport(managerId, start, end)).thenReturn(reportData);
    when(workloadGenerator.generate(reportData, format)).thenReturn(expectedBytes);

    byte[] result = reportFacade.getTeamWorkload(managerId, start, end, format);

    assertThat(result).isEqualTo(expectedBytes);
    verify(workloadService).generateReport(managerId, start, end);
    verify(workloadGenerator).generate(reportData, format);
  }

  @Test
  void shouldGetTeamAbsences() {
    Integer managerId = 1;
    LocalDate start = LocalDate.now();
    LocalDate end = LocalDate.now();
    ReportFormat format = ReportFormat.XLSX;
    byte[] expectedBytes = {7, 8, 9};
    TeamAbsencesReport reportData = mock(TeamAbsencesReport.class);

    when(absenceService.generateReport(managerId, start, end)).thenReturn(reportData);
    when(absenceGenerator.generate(reportData, format)).thenReturn(expectedBytes);

    byte[] result = reportFacade.getTeamAbsences(managerId, start, end, format);

    assertThat(result).isEqualTo(expectedBytes);
    verify(absenceService).generateReport(managerId, start, end);
    verify(absenceGenerator).generate(reportData, format);
  }

  @Test
  void shouldGetEmployeeOvertime() {
    Integer employeeId = 1;
    LocalDate start = LocalDate.now();
    LocalDate end = LocalDate.now();
    ReportFormat format = ReportFormat.PDF;
    byte[] expectedBytes = {10, 11, 12};
    EmployeeOvertimeReport reportData = mock(EmployeeOvertimeReport.class);

    when(overtimeService.generateEmployeeReport(employeeId, start, end)).thenReturn(reportData);
    when(employeeOvertimeGenerator.generate(reportData, format)).thenReturn(expectedBytes);

    byte[] result = reportFacade.getEmployeeOvertime(employeeId, start, end, format);

    assertThat(result).isEqualTo(expectedBytes);
    verify(overtimeService).generateEmployeeReport(employeeId, start, end);
    verify(employeeOvertimeGenerator).generate(reportData, format);
  }

  @Test
  void shouldGetTeamOvertime() {
    Integer managerId = 1;
    LocalDate start = LocalDate.now();
    LocalDate end = LocalDate.now();
    ReportFormat format = ReportFormat.CSV;
    byte[] expectedBytes = {13, 14, 15};
    TeamOvertimeReport reportData = mock(TeamOvertimeReport.class);

    when(overtimeService.generateTeamReport(managerId, start, end)).thenReturn(reportData);
    when(teamOvertimeGenerator.generate(reportData, format)).thenReturn(expectedBytes);

    byte[] result = reportFacade.getTeamOvertime(managerId, start, end, format);

    assertThat(result).isEqualTo(expectedBytes);
    verify(overtimeService).generateTeamReport(managerId, start, end);
    verify(teamOvertimeGenerator).generate(reportData, format);
  }

  @Test
  void shouldGetTeamAnomalyReport() {
    Integer managerId = 1;
    LocalDate start = LocalDate.now();
    LocalDate end = LocalDate.now();
    ReportFormat format = ReportFormat.XLSX;
    byte[] expectedBytes = {16, 17, 18};
    TeamAnomalyReport reportData = mock(TeamAnomalyReport.class);

    when(anomalyService.generateReport(managerId, start, end)).thenReturn(reportData);
    when(anomalyGenerator.generate(reportData, format)).thenReturn(expectedBytes);

    byte[] result = reportFacade.getTeamAnomalyReport(managerId, start, end, format);

    assertThat(result).isEqualTo(expectedBytes);
    verify(anomalyService).generateReport(managerId, start, end);
    verify(anomalyGenerator).generate(reportData, format);
  }
}
