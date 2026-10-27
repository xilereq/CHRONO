package pl.chrono.reportmanager.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.chrono.reportmanager.api.I_ScheduledReportService;

@ExtendWith(MockitoExtension.class)
class ReportSchedulerJobTest {

  @Mock
  private I_ScheduledReportService scheduledReportService;

  @InjectMocks
  private ReportSchedulerJob reportSchedulerJob;

  @Test
  void checkOverdueReportsOnStartupTest() {
    reportSchedulerJob.checkOverdueReportsOnStartup();

    verify(scheduledReportService).processDueSchedules();
  }

  @Test
  void executeScheduledReportsTest() {
    reportSchedulerJob.executeScheduledReports();

    verify(scheduledReportService).processDueSchedules();
  }
}
