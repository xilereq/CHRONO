package pl.chrono.dbmanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "holiday", indexes = {
    @Index(name = "idx_holiday_date", columnList = "date")
})
@Getter
@Setter
public class Holiday {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "holiday_id")
  private Integer holidayId;

  @Column(length = 100)
  private String name;

  private LocalDate date;
}
