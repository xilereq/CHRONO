package pl.chrono.dbmanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


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

    @OneToMany(mappedBy = "policy")
    private List<UsersPolicy> userPolicies;

    @OneToMany(mappedBy = "policy")
    private List<HolidayPolicy> holidayPolicies;
}