package pl.chrono.dbmanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "holiday")
@Getter
@Setter
public class Holiday {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "holiday_id")
  private Integer holidayId;

  @Column(length = 100)
  private String name;

  private boolean occursYearly;

  private LocalDate date;

  @OneToMany(mappedBy = "holiday")
  private List<HolidayPolicy> holidayPolicies;
}
