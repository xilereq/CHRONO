package pl.chrono.dbmanager.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

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