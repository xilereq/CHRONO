package pl.chrono.dbmanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User implements UserDetails {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "user_id")
  private Integer userId;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 100)
  private String surname;

  @Column(nullable = false, unique = true, length = 100)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 256)
  private String passwordHash;

  @Column(columnDefinition = "TEXT")
  private String refreshToken;

  private LocalDateTime refreshExpiration;

  @Column(name = "force_password_change")
  private boolean forcePasswordChange = true;

  @Column(name = "reset_token")
  private String resetToken;

  @Column(name = "reset_token_expiration")
  private LocalDateTime resetTokenExpiration;


  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "user_roles",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id")
  )
  private Set<Role> roles = new HashSet<>();

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

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return roles.stream()
        .map(role -> new SimpleGrantedAuthority(role.getName()))
        .toList();
  }
  @Override
  public String getPassword() {
    return this.passwordHash;
  }

  @Override
  public String getUsername() {
    return this.email;
  }

  public Policy getActivePolicy() {
    if (this.userPolicies == null || this.userPolicies.isEmpty()) {
      return null;
    }
    LocalDate now = LocalDate.now();
    return this.userPolicies.stream()
        .filter(up -> (up.getValidFrom() == null || !now.isBefore(up.getValidFrom())) &&
                      (up.getValidTo() == null || !now.isAfter(up.getValidTo())))
        .map(UsersPolicy::getPolicy)
        .findFirst()
        .orElse(null);
  }
}
