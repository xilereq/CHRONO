package pl.chrono.dbmanager.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.chrono.dbmanager.model.OvertimeArchive;
import pl.chrono.dbmanager.ports.I_OvertimeData;
import pl.chrono.dbmanager.repository.OvertimeArchiveRepository;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OvertimeDataAdapter implements I_OvertimeData {
  private final OvertimeArchiveRepository repository;

  @Override
  public Integer getUserOvertimeBalance(Integer userId) {
    return repository.calculateOvertimeBalanceSeconds(userId);
  }

  @Override
  public OvertimeArchive saveOvertimeRecord(OvertimeArchive record) {
    return repository.save(record);
  }

  @Override
  public List<OvertimeArchive> findUserOvertimeHistory(Integer userId) {
    return repository.findByUser_UserId(userId);
  }
}
