package com.example.backend.service;

import com.example.backend.dto.SignupRequest;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.enums.UserStatus;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.regex.Pattern;

@Service
@Transactional
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void signup(SignupRequest signupRequest) {
        validateSignupRequest(signupRequest);

        if (userRepository.findUserByEmail(signupRequest.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        if (userRepository.findUserByPhoneNumber(signupRequest.getPhoneNumber()).isPresent()) {
            throw new IllegalArgumentException("Phone number already registered");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Default role USER not found - please seed database"));

        User user = new User();
        user.setFullName(signupRequest.getFullName().trim());
        user.setEmail(signupRequest.getEmail().trim().toLowerCase());
        user.setPhoneNumber(signupRequest.getPhoneNumber().trim());
        user.setGender(UserStatus.valueOf(signupRequest.getGender().toUpperCase()));
        user.setHashedPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setBirthDate(LocalDate.parse(signupRequest.getDateOfBirth()));
        user.setRole(userRole);
        user.setEnabled(true);
        user.setLocked(false);
        user.setTokenVersion(0);
        user.setFailed(0);

        userRepository.save(user);
        logger.info("User registered successfully: {}", user.getEmail());
    }

    private void validateSignupRequest(SignupRequest req) {
        if (req == null) throw new IllegalArgumentException("Signup request is required");
        if (isBlank(req.getEmail())) throw new IllegalArgumentException("Email is required");
        if (!EMAIL_PATTERN.matcher(req.getEmail()).matches()) throw new IllegalArgumentException("Invalid email format");
        if (isBlank(req.getPassword())) throw new IllegalArgumentException("Password is required");
        if (req.getPassword().length() < 6) throw new IllegalArgumentException("Password must be at least 6 characters");
        if (isBlank(req.getFullName())) throw new IllegalArgumentException("Full name is required");
        if (isBlank(req.getPhoneNumber())) throw new IllegalArgumentException("Phone number is required");
        if (isBlank(req.getDateOfBirth())) throw new IllegalArgumentException("Date of birth is required");
        if (isBlank(req.getGender())) throw new IllegalArgumentException("Gender is required");
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isBlank();
    }
}
