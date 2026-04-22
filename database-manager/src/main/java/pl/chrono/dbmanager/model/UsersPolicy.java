package pl.chrono.dbmanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

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
