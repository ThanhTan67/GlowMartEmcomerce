package com.example.backend.service;

import com.example.backend.dto.SignupRequest;
import com.example.backend.entity.User;
import com.example.backend.enums.UserStatus;
import com.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public String signup(SignupRequest signupRequest) {
        if(userRepository.existsUserByEmail(signupRequest.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        if(userRepository.existsUserByPhone(signupRequest.getPhoneNumber()).isPresent()) {
            throw new RuntimeException("Phone number already exists");
        }

        User user = new User();
        user.setFullName(signupRequest.getFullName());
        user.setEmail(signupRequest.getEmail());
        user.setPhoneNumber(signupRequest.getPhoneNumber());
        user.setGender(UserStatus.valueOf(signupRequest.getGender()));
        user.setHashedPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setBirthDate(LocalDate.parse(signupRequest.getDateOfBirth()));

        return "Signup successful";
    }

}
