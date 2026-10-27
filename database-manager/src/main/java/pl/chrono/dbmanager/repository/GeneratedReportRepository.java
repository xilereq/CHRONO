package pl.chrono.dbmanager.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.chrono.dbmanager.model.GeneratedReport;
import pl.chrono.dbmanager.model.GeneratedReportInfo;

@Repository
public interface GeneratedReportRepository extends JpaRepository<GeneratedReport, Long> {
  List<GeneratedReportInfo> findBySubjectUserIdOrderByCreatedAtDesc(Integer subjectUserId);
}
