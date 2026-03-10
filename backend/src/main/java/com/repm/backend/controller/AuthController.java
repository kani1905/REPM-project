package com.repm.backend.controller;

import com.repm.backend.entity.User;
import com.repm.backend.entity.UserSource;
import com.repm.backend.repository.UserRepository;
import com.repm.backend.repository.UserSourceRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSourceRepository userSourceRepository;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          UserSourceRepository userSourceRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userSourceRepository = userSourceRepository;
    }

    // ===== REGISTER =====

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Username already exists!");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (request.getSources() == null || request.getSources().isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Select at least one energy source.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.USER);

        userRepository.save(user);

        for (String src : request.getSources()) {
            UserSource userSource = new UserSource();
            userSource.setUserId(user.getId());
            userSource.setSourceName(src);
            userSourceRepository.save(userSource);
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Registration successful!");
        return ResponseEntity.ok(response);
    }
    
    public static class RegisterRequest {
        private String username;
        private String password;
        private List<String> sources;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public List<String> getSources() { return sources; }
        public void setSources(List<String> sources) { this.sources = sources; }
    }

    // ===== FORGOT PASSWORD API =====

    @PostMapping("/forgot-password")
    public ResponseEntity<?> processForgotPassword(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        Optional<User> optionalUser = userRepository.findByUsername(username);

        if (optionalUser.isPresent()) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Username found. Proceed to reset password.");
            return ResponseEntity.ok(response);
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Username not found!");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ===== RESET PASSWORD API =====

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {

        Optional<User> optionalUser = userRepository.findByUsername(request.getUsername());

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            userRepository.save(user);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Password updated successfully!");
            return ResponseEntity.ok(response);
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Username not found!");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    public static class ResetPasswordRequest {
        private String username;
        private String password;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
