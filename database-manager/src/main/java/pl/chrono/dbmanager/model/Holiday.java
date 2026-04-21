package pl.chrono.dbmanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;


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
