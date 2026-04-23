package pl.chrono.dbmanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
public class Role {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // for Spring Security it must start with "ROLE_"
  // for example ROLE_EMPLOYEE, ROLE_MANAGER, ROLE_HR
  @Column(nullable = false, unique = true)
  private String name;
}
