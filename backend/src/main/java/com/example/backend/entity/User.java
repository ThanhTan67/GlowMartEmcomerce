package com.example.backend.entity;

import com.example.backend.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "users")
public class User extends BaseEntity {
    @Column(name = "full_name")
    private String fullName;
    @Column(name = "hashed_password")
    private String hashedPassword;
    private String email;
    @Column(name = "phone_number")
    private String phoneNumber;
    private UserStatus gender;
    @Column(name = "birth_date")
    private LocalDate birthDate;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    @ColumnDefault("0")
    @Column(name = "failed", nullable = false)
    private Integer failed = 0;
    @ColumnDefault("false")
    @Column(name = "locked", nullable = false)
    private Boolean locked = false;
    @Column(name = "lock_time")
    private Instant lockTime;
    @Column(name = "token_version", length = 512)
    private String tokenVersion;

}
