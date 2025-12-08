package com.example.backend.service;

import com.example.backend.dto.AuthResponse;
import com.example.backend.dto.LoginRequest;
import com.example.backend.dto.SignupRequest;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.enums.GenderOptions;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_DURATION_SECONDS = 300; // 5 phút

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    public void signup(SignupRequest request) {
        validateSignupRequest(request);

        if (userRepository.findUserByEmail(request.getEmail().trim().toLowerCase()).isPresent()) {
            throw new IllegalArgumentException("Email đã được đăng ký");
        }
        if (userRepository.findUserByPhoneNumber(request.getPhoneNumber().trim()).isPresent()) {
            throw new IllegalArgumentException("Số điện thoại đã được đăng ký");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Role USER không tồn tại - vui lòng seed dữ liệu"));

        GenderOptions gender = GenderOptions.valueOf(request.getGender().trim().toUpperCase());

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPhoneNumber(request.getPhoneNumber().trim());
        user.setGender(gender);
        user.setBirthDate(LocalDate.parse(request.getDateOfBirth()));
        user.setHashedPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(userRole);
        user.setEnabled(true);
        user.setLocked(false);
        user.setFailed(0);
        user.setTokenVersion(0);

        userRepository.save(user);
        logger.info("Đăng ký thành công: {}", user.getEmail());
    }

    public AuthResponse login(LoginRequest req) {
        String email = req.getEmail() != null ? req.getEmail().trim().toLowerCase() : null;
        String phone = req.getPhoneNumber() != null ? req.getPhoneNumber().trim() : null;

        // Bắt buộc chỉ được nhập 1 trong 2
        if ((email == null || email.isBlank()) && (phone == null || phone.isBlank())) {
            throw new IllegalArgumentException("Vui lòng nhập email hoặc số điện thoại");
        }
        if (email != null && !email.isBlank() && phone != null && !phone.isBlank()) {
            throw new IllegalArgumentException("Chỉ được nhập email hoặc số điện thoại, không nhập cả hai");
        }

        User user = email != null
                ? userRepository.findUserByEmail(email).orElse(null)
                : userRepository.findUserByPhoneNumber(phone).orElse(null);

        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getHashedPassword())) {
            handleFailedAttempt(user);
            throw new IllegalArgumentException("Sai thông tin đăng nhập");
        }

        checkAndUnlockAccount(user);
        user.setFailed(0);
        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getEmail(),
                user.getId(),
                user.getTokenVersion()
        );

        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getEmail(),
                user.getId(),
                user.getTokenVersion()
        );

        logger.info("Login thành công: {}", user.getEmail());
        return new AuthResponse(accessToken, refreshToken);
    }

    // Helper: xử lý khi đăng nhập sai
    private void handleFailedAttempt(User user) {
        if (user != null) {
            user.setFailed(user.getFailed() + 1);
            if (user.getFailed() >= 5) {
                user.setLocked(true);
                user.setLockTime(Instant.now().plusSeconds(300)); // khóa 5 phút
                logger.warn("Tài khoản bị khóa do đăng nhập sai quá 5 lần: {}", user.getEmail());
            }
            userRepository.save(user);
        }
    }

    // Helper: tự động mở khóa nếu hết thời gian
    private void checkAndUnlockAccount(User user) {
        if (user.getLocked() && user.getLockTime() != null && Instant.now().isAfter(user.getLockTime())) {
            user.setLocked(false);
            user.setLockTime(null);
            user.setFailed(0);
            userRepository.save(user);
        }
        if (user.getLocked()) {
            throw new IllegalArgumentException("Tài khoản bị khóa tạm thời. Vui lòng thử lại sau 5 phút.");
        }
        if (!user.getEnabled()) {
            throw new IllegalArgumentException("Tài khoản đã bị vô hiệu hóa");
        }
    }

    private void validateSignupRequest(SignupRequest req) {
        if (req == null) throw new IllegalArgumentException("Yêu cầu đăng ký không được để trống");
        if (isBlank(req.getEmail()) || !EMAIL_PATTERN.matcher(req.getEmail()).matches())
            throw new IllegalArgumentException("Email không hợp lệ");
        if (isBlank(req.getPassword()) || req.getPassword().length() < 8)
            throw new IllegalArgumentException("Mật khẩu phải từ 8 ký tự trở lên");
        if (isBlank(req.getFullName())) throw new IllegalArgumentException("Họ tên không được để trống");
        if (isBlank(req.getPhoneNumber())) throw new IllegalArgumentException("Số điện thoại không được để trống");
        if (isBlank(req.getDateOfBirth())) throw new IllegalArgumentException("Ngày sinh không được để trống");
        if (isBlank(req.getGender())) throw new IllegalArgumentException("Giới tính không được để trống");
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isBlank();
    }
}