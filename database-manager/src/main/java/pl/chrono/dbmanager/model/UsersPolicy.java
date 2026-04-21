package pl.chrono.dbmanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users_policy")
@Getter
@Setter
public class UsersPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userPolicyId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Column(nullable = false)
    private LocalDateTime validFrom;

    private LocalDateTime validTo;
}
