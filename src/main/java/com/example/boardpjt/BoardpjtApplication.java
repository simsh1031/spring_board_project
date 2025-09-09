package com.example.boardpjt;

import com.example.boardpjt.model.entity.UserAccount;
import com.example.boardpjt.model.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class BoardpjtApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoardpjtApplication.class, args);
    }

    @Bean
    public CommandLineRunner initAdmin(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            String adminUsername = "admin";
            String adminPassword = "pass";
            ;
            boolean adminExists = userAccountRepository.findByUsername(adminUsername).isPresent();

            if (adminExists) {
                UserAccount oldAdmin = userAccountRepository.findByUsername(adminUsername).get();
                userAccountRepository.delete(oldAdmin);
            }

            UserAccount admin = new UserAccount();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole("ROLE_ADMIN");
            userAccountRepository.save(admin);
        };
    }
}
