package pl.chrono.dbmanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  private Integer userId;

  @Column(nullable = false, unique = true, length = 100)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 256)
  private String passwordHash;

  @Column(columnDefinition = "TEXT")
  private String refreshToken;

  private LocalDateTime refreshExpiration;

  @Column(nullable = false, length = 10)
  private String role;

  private Integer overtimeBalance;

  @ManyToOne
  @JoinColumn(name = "manager_id")
  private User manager;

  @OneToMany(mappedBy = "manager")
  private List<User> subordinates;

  @OneToMany(mappedBy = "user")
  private List<WorkLog> workLogs;

  @OneToMany(mappedBy = "user")
  private List<LeaveLog> leaveLogs;

  @OneToMany(mappedBy = "user")
  private List<UsersPolicy> userPolicies;
}
