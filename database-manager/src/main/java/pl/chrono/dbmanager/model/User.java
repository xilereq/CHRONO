package pl.chrono.dbmanager.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
          name = "user_roles",
          joinColumns = @JoinColumn(name = "user_id"),
          inverseJoinColumns = @JoinColumn(name = "role_id")
  )
  private Set<Role> roles = new HashSet<>();

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
