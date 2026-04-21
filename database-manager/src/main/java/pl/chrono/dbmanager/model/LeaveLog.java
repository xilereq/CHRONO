package pl.chrono.dbmanager.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "leave_log")
@Getter
@Setter
public class LeaveLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leave_log_id")
    private Integer leaveLogId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Column(length = 15)
    private String status;

    @Column(length = 15)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String description;
}
