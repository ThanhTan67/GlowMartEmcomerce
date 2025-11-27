package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.backend.enums.UserStatus;

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
    private String salt;
    private String email;
    @Column(name = "phone_number")
    private String phoneNumber;
    private Enum<UserStatus> gender;
    @Column(name = "birth_date")
    private LocalDate birthDate;
}
