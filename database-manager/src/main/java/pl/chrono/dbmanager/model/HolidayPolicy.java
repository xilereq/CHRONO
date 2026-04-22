package pl.chrono.dbmanager.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "holiday_policy")
@Getter
@Setter
public class HolidayPolicy {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer holidayPolicyId;

  @ManyToOne
  @JoinColumn(name = "holiday_id", nullable = false)
  private Holiday holiday;

  @ManyToOne
  @JoinColumn(name = "policy_id", nullable = false)
  private Policy policy;
}
