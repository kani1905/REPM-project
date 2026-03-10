package com.repm.backend.config;

import com.repm.backend.entity.User;
import com.repm.backend.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner init(UserRepository userRepository) {

        return args -> {

            if (userRepository.findByUsername("admin").isEmpty()) {

                User admin = new User();

                admin.setUsername("admin");

                admin.setPassword("admin123"); // NoOpPasswordEncoder allows plain text

                admin.setRole(User.Role.ADMIN);

                userRepository.save(admin);

                System.out.println("Admin created: admin / admin123");
            }
        };
    }
}