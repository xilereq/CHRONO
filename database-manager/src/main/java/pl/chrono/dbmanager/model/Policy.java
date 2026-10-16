package pl.chrono.dbmanager.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "policy")
@Getter
@Setter
public class Policy {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "policy_id")
  private Integer policyId;

  @Column(length = 100)
  private String name;

  @Column(nullable = false)
  private Integer dailyHours;

  private Integer maxDailyHours;

  private Integer maxWeeklyHours;

  private Integer roundingMinutes;

  @OneToMany(mappedBy = "policy")
  private List<UsersPolicy> userPolicies;

  @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<HolidayPolicy> holidayPolicies = new ArrayList<>();

  @ElementCollection
  @CollectionTable(name = "policy_working_days", joinColumns = @JoinColumn(name = "policy_id"))
  @Column(name = "day_of_week")
  private Set<DayOfWeek> workingDays = new HashSet<>();

  public int getRequiredSecondsForDate(LocalDate date) {
    if (date == null) {
      return 0;
    }
    if (!workingDays.contains(date.getDayOfWeek())) {
      return 0;
    }
    if (holidayPolicies != null) {
      boolean isHoliday = holidayPolicies.stream()
          .map(HolidayPolicy::getHoliday)
          .filter(holiday -> holiday != null && holiday.getDate() != null)
          .anyMatch(holiday -> date.equals(holiday.getDate()));
      if (isHoliday) {
        return 0;
      }
    }
    return (dailyHours != null ? dailyHours : 8) * 3600;
  }

  public int getMaxAllowedSeconds() {
    return maxDailyHours != null ? maxDailyHours * 3600 : 12 * 3600;
  }
}
