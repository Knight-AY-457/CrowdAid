package com.crowdaid.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.crowdaid.backend.user.AppUser;
import com.crowdaid.backend.user.UserRepository;
import com.crowdaid.backend.user.UserRole;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.email}")
    private String adminEmail;

    @Value("${app.bootstrap.admin.password}")
    private String adminPassword;

    @Value("${app.bootstrap.admin.first-name}")
    private String adminFirstName;

    @Value("${app.bootstrap.admin.last-name}")
    private String adminLastName;

    @Value("${app.bootstrap.admin.phone}")
    private String adminPhone;

    @Override
    public void run(ApplicationArguments args) {
        var existingAdmin = userRepository.findByEmailIgnoreCase(adminEmail);
        if (existingAdmin.isPresent()) {
            AppUser admin = existingAdmin.get();
            boolean changed = false;

            if (!passwordEncoder.matches(adminPassword, admin.getPasswordHash())) {
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                changed = true;
            }
            if (admin.getRole() != UserRole.ADMIN) {
                admin.setRole(UserRole.ADMIN);
                changed = true;
            }
            if (admin.isVolunteer()) {
                admin.setVolunteer(false);
                changed = true;
            }
            if (!admin.isVerified()) {
                admin.setVerified(true);
                changed = true;
            }
            if (!adminPhone.equals(admin.getPhone())) {
                admin.setPhone(adminPhone);
                changed = true;
            }
            if (!adminFirstName.equals(admin.getFirstName())) {
                admin.setFirstName(adminFirstName);
                changed = true;
            }
            if (!adminLastName.equals(admin.getLastName())) {
                admin.setLastName(adminLastName);
                changed = true;
            }

            if (changed) {
                userRepository.save(admin);
            }
            return;
        }

        AppUser admin = new AppUser();
        admin.setFirstName(adminFirstName);
        admin.setLastName(adminLastName);
        admin.setEmail(adminEmail.toLowerCase());
        admin.setPhone(adminPhone);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(UserRole.ADMIN);
        admin.setVolunteer(false);
        admin.setVerified(true);
        admin.setOnline(false);

        userRepository.save(admin);
    }
}
