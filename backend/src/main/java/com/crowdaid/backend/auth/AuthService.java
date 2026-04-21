package com.crowdaid.backend.auth;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.crowdaid.backend.auth.dto.AuthResponseDto;
import com.crowdaid.backend.auth.dto.ChangePasswordDto;
import com.crowdaid.backend.auth.dto.DeleteAccountRequestDto;
import com.crowdaid.backend.auth.dto.LoginRequestDto;
import com.crowdaid.backend.auth.dto.LogoutRequestDto;
import com.crowdaid.backend.auth.dto.RefreshTokenRequestDto;
import com.crowdaid.backend.auth.dto.RegisterRequestDto;
import com.crowdaid.backend.auth.dto.ResetPasswordWithSecurityDto;
import com.crowdaid.backend.auth.dto.UserDto;
import com.crowdaid.backend.auth.dto.UserSessionDto;
import com.crowdaid.backend.common.ApiException;
import com.crowdaid.backend.emergency.EmergencyRepository;
import com.crowdaid.backend.emergency.EmergencyStatus;
import com.crowdaid.backend.security.SessionTokenService;
import com.crowdaid.backend.user.AppUser;
import com.crowdaid.backend.user.UserRepository;
import com.crowdaid.backend.user.UserRole;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Pattern TEN_DIGIT_PATTERN = Pattern.compile("\\d{10}");

    private final UserRepository userRepository;
    private final EmergencyRepository emergencyRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionTokenService sessionTokenService;

    @Transactional
    public AuthResponseDto register(RegisterRequestDto request, String ipAddress, String userAgent) {
        if (!Boolean.TRUE.equals(request.agreeToTerms()) || !Boolean.TRUE.equals(request.agreeToEmergencyContact())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You must accept required terms");
        }

        String email = request.email().toLowerCase(Locale.ROOT);
        String phone = normalizePhone(request.phone());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "Phone number already registered");
        }

        AppUser user = new AppUser();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDateOfBirth(request.dateOfBirth());
        user.setSecurityQuestion(normalizeSecurityQuestion(request.securityQuestion()));
        user.setSecurityAnswerHash(passwordEncoder.encode(normalizeSecurityAnswer(request.securityAnswer())));
        user.setRole(Boolean.TRUE.equals(request.isVolunteer()) ? UserRole.VOLUNTEER : UserRole.USER);
        user.setVolunteer(Boolean.TRUE.equals(request.isVolunteer()));
        user.setVerified(true);
        user.setOnline(false);

        AppUser saved = userRepository.save(user);

        SessionTokenService.TokenPair tokenPair = sessionTokenService.issueTokens(saved, safeIp(ipAddress), userAgent);
        return new AuthResponseDto(tokenPair.accessToken(), tokenPair.refreshToken(), toUserDto(saved));
    }

    @Transactional
    public void resetForgottenPassword(ResetPasswordWithSecurityDto request, String ipAddress) {
        String phone = normalizePhone(request.phone());

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }

        AppUser user = userRepository.findByPhone(phone).orElse(null);
        if (user == null || !matchesSecurityProfile(user, request.dateOfBirth(), request.securityQuestion(), request.securityAnswer())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid recovery details");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        sessionTokenService.revokeAllSessions(user.getId());
    }

    @Transactional
    public void changePassword(AppUser user, ChangePasswordDto request, String ipAddress) {
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }

        if (!matchesSecurityProfile(user, request.dateOfBirth(), request.securityQuestion(), request.securityAnswer())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid security verification details");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        sessionTokenService.revokeAllSessions(user.getId());
    }

    public AuthResponseDto login(LoginRequestDto request, String ipAddress, String userAgent) {
        AppUser user = userRepository.findByEmailIgnoreCase(request.email())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        SessionTokenService.TokenPair tokenPair = sessionTokenService.issueTokens(user, safeIp(ipAddress), userAgent);
        return new AuthResponseDto(tokenPair.accessToken(), tokenPair.refreshToken(), toUserDto(user));
    }

    @Transactional
    public AuthResponseDto refresh(RefreshTokenRequestDto request, String ipAddress, String userAgent) {
        SessionTokenService.RefreshResult result = sessionTokenService.refreshTokens(
            request.refreshToken(),
            safeIp(ipAddress),
            userAgent
        );
        return new AuthResponseDto(result.accessToken(), result.refreshToken(), toUserDto(result.user()));
    }

    @Transactional
    public void logout(AppUser user, String accessToken, LogoutRequestDto request) {
        String refreshToken = request == null ? null : request.refreshToken();
        sessionTokenService.revokeRefreshToken(refreshToken);
        sessionTokenService.revokeAccessToken(accessToken, user);
    }

    @Transactional
    public void logoutAll(AppUser user, String accessToken) {
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        sessionTokenService.revokeAllSessions(user.getId());
        sessionTokenService.revokeAccessToken(accessToken, user);
    }

    public List<UserSessionDto> sessions(AppUser user) {
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return sessionTokenService.listRecentSessions(user.getId()).stream()
            .map(item -> new UserSessionDto(
                item.getId(),
                item.getIpAddress(),
                item.getUserAgent(),
                item.getCreatedAt(),
                item.getExpiresAt(),
                item.isRevoked()
            ))
            .toList();
    }

    @Transactional
    public void deleteAccount(AppUser user, DeleteAccountRequestDto request, String accessToken) {
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid password");
        }

        boolean hasActiveSos = emergencyRepository.existsByUserIdAndStatusIn(
            user.getId(),
            List.of(EmergencyStatus.PENDING, EmergencyStatus.IN_PROGRESS)
        );
        if (hasActiveSos) {
            throw new ApiException(HttpStatus.CONFLICT, "Resolve or cancel active SOS requests before deleting account");
        }
        if (emergencyRepository.countByRespondingVolunteerIdAndStatus(user.getId(), EmergencyStatus.IN_PROGRESS) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Complete your active volunteer responses before deleting account");
        }

        sessionTokenService.revokeAllSessions(user.getId());
        sessionTokenService.revokeAccessToken(accessToken, user);
        userRepository.delete(user);
    }

    public UserDto me(AppUser user) {
        if (user == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return toUserDto(user);
    }

    public UserDto toUserDto(AppUser user) {
        return new UserDto(
            user.getId(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getPhone(),
            user.getRole(),
            user.isVolunteer(),
            user.isVerified(),
            user.getVolunteerRating() == null ? null : user.getVolunteerRating().doubleValue(),
            user.getThankPointsTotal(),
            user.getTotalHelped(),
            user.getCompletionRate() == null ? null : user.getCompletionRate().doubleValue(),
            user.getAvgResponseTime(),
            user.isOnline(),
            user.getCreatedAt()
        );
    }

    private boolean matchesSecurityProfile(AppUser user, LocalDate dateOfBirth, String securityQuestion, String securityAnswer) {
        if (user.getDateOfBirth() == null || user.getSecurityQuestion() == null || user.getSecurityAnswerHash() == null) {
            return false;
        }

        if (!user.getDateOfBirth().equals(dateOfBirth)) {
            return false;
        }

        String storedQuestion = normalizeSecurityQuestion(user.getSecurityQuestion()).toLowerCase(Locale.ROOT);
        String providedQuestion = normalizeSecurityQuestion(securityQuestion).toLowerCase(Locale.ROOT);
        if (!storedQuestion.equals(providedQuestion)) {
            return false;
        }

        return passwordEncoder.matches(normalizeSecurityAnswer(securityAnswer), user.getSecurityAnswerHash());
    }

    private String normalizeSecurityQuestion(String question) {
        if (question == null) {
            return "";
        }
        return question.trim().replaceAll("\\s+", " ");
    }

    private String normalizeSecurityAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        return answer.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        String raw = phone == null ? "" : phone.trim();
        raw = raw.replaceAll("[\\s\\-()]", "");

        String digits;
        if (raw.startsWith("+")) {
            if (!raw.startsWith("+91")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Only Indian phone numbers (+91) are supported");
            }
            digits = raw.substring(3);
        } else {
            digits = raw;
        }

        if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }

        if (!TEN_DIGIT_PATTERN.matcher(digits).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Phone number must have 10 digits");
        }

        return "+91" + digits;
    }

    private String safeIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return "unknown";
        }
        return ipAddress.trim();
    }
}