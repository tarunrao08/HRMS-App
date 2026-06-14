// Seeds a default HR Admin user on first startup when the users table is empty
package com.hrms.common;

import com.hrms.auth.entity.Role;
import com.hrms.auth.entity.User;
import com.hrms.auth.repository.RoleRepository;
import com.hrms.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository    userRepository;
    private final RoleRepository    roleRepository;
    private final PasswordEncoder   passwordEncoder;

    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_EMAIL    = "admin@hrms.com";
    private static final String DEFAULT_PASSWORD = "Admin@1234";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        Role adminRole = roleRepository.findByName(Constants.Roles.HR_ADMIN)
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                            .name(Constants.Roles.HR_ADMIN)
                            .description("HR Administrator — full system access")
                            .build()
                ));

        User admin = User.builder()
                .username(DEFAULT_USERNAME)
                .email(DEFAULT_EMAIL)
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .build();
        admin.getRoles().add(adminRole);
        userRepository.save(admin);

        log.info("==========================================================");
        log.info("  Default admin user created:");
        log.info("  Username : {}", DEFAULT_USERNAME);
        log.info("  Password : {}", DEFAULT_PASSWORD);
        log.info("  Email    : {}", DEFAULT_EMAIL);
        log.info("  Change the password after first login!");
        log.info("==========================================================");
    }
}
