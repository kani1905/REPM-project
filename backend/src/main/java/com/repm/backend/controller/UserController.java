package com.repm.backend.controller;

import com.repm.backend.service.UserService;
import com.repm.backend.entity.EnergyData;
import com.repm.backend.entity.User;
import com.repm.backend.entity.UserSource;
import com.repm.backend.repository.UserRepository;
import com.repm.backend.repository.UserSourceRepository;
import com.repm.backend.service.NotificationService;
import com.repm.backend.service.PdfReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final UserSourceRepository userSourceRepository;
    private final PdfReportService pdfReportService;
    private final com.repm.backend.repository.NotificationRepository notificationRepository;

    public UserController(UserService userService,
                          NotificationService notificationService,
                          UserRepository userRepository,
                          UserSourceRepository userSourceRepository,
                          PdfReportService pdfReportService,
                          com.repm.backend.repository.NotificationRepository notificationRepository) {
        this.userService = userService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.userSourceRepository = userSourceRepository;
        this.pdfReportService = pdfReportService;
        this.notificationRepository = notificationRepository;
    }

    private List<String> getAllowedSourcesFromDB(User user) {
        return userSourceRepository.findByUserId(user.getId())
                .stream()
                .map(UserSource::getSourceName)
                .toList();
    }

    // ================= DASHBOARD =================

   @GetMapping("/dashboard")
public ResponseEntity<?> dashboard(Principal principal) {

    if (principal == null) {
        return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
    }

    String username = principal.getName();

    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

    List<String> allowed = getAllowedSourcesFromDB(user);

    // Get today's data
    List<EnergyData> todayList = userService.getTodayDataList(username)
            .stream()
            .filter(d -> allowed.contains(d.getSource()))
            .toList();

    // ===== Keep only ONE entry per source =====
    Map<String, EnergyData> uniqueSources = new HashMap<>();

    for (EnergyData d : todayList) {
        uniqueSources.putIfAbsent(d.getSource(), d);
    }

    List<EnergyData> filteredList = uniqueSources.values().stream().toList();

    // ===== Calculations =====
    double totalProduced = filteredList.stream()
            .mapToDouble(EnergyData::getEnergyProduced)
            .sum();

    double totalConsumed = filteredList.stream()
            .mapToDouble(EnergyData::getEnergyConsumed)
            .sum();

    double avgEfficiency = filteredList.stream()
            .mapToDouble(EnergyData::getEfficiency)
            .average()
            .orElse(0);

    double totalBill = filteredList.stream()
            .mapToDouble(EnergyData::getElectricityBill)
            .sum();

    Map<String, Object> data = new HashMap<>();
    data.put("username", username);
    data.put("theme", user.getTheme());
    data.put("profileImage", user.getProfileImage());
    data.put("todayDataList", filteredList);
    data.put("totalProduced", totalProduced);
    data.put("totalConsumed", totalConsumed);
    data.put("avgEfficiency", avgEfficiency);
    data.put("totalBill", totalBill);
    data.put("allowedSources", allowed);

    return ResponseEntity.ok(data);
}

    // ================= INPUT =================

    @GetMapping("/input")
    public ResponseEntity<?> inputPage(Principal principal) {
        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(Map.of("allowedSources", getAllowedSourcesFromDB(user)));
    }

    @PostMapping("/input")
public ResponseEntity<?> submitInput(@RequestBody Map<String, Object> payload,
                                     Principal principal) {

    String username = principal.getName();

    String source = payload.get("source").toString();

    Double energyProduced = Double.valueOf(payload.get("energyProduced").toString());
    Double energyConsumed = Double.valueOf(payload.get("energyConsumed").toString());
    Double systemCapacity = Double.valueOf(payload.get("systemCapacity").toString());
    Double operatingHours = Double.valueOf(payload.get("operatingHours").toString());
    Double electricityRate = Double.valueOf(payload.get("electricityRate").toString());

    userService.saveTodayData(
            username,
            source,
            energyProduced,
            energyConsumed,
            systemCapacity,
            operatingHours,
            electricityRate
    );

    return ResponseEntity.ok(Map.of("message", "Energy data saved successfully"));
}

    // ================= SETTINGS =================

    @GetMapping("/settings")
    public ResponseEntity<?> settingsPage(Principal principal) {
        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> data = new HashMap<>();
        data.put("username", user.getUsername());
        data.put("email", user.getEmail());
        data.put("fullName", user.getFullName());
        data.put("phone", user.getPhone());
        data.put("theme", user.getTheme());
        data.put("profileImage", user.getProfileImage());

        return ResponseEntity.ok(data);
    }

    @PostMapping("/settings")
    public ResponseEntity<?> updateSettings(@RequestBody Map<String, Object> payload,
                                             Principal principal) {
        String email = payload.getOrDefault("email", "").toString();
        String password = payload.containsKey("password") ? payload.get("password").toString() : null;
        String theme = payload.containsKey("theme") ? payload.get("theme").toString() : null;

        userService.updateUserSettings(principal.getName(), email, password, theme);

        return ResponseEntity.ok(Map.of("message", "Settings updated"));
    }

    // ================= PROFILE IMAGE =================

    @PostMapping("/upload-profile")
    public ResponseEntity<?> uploadProfile(@RequestParam("file") MultipartFile file,
                                            Principal principal) throws IOException {
        String username = principal.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!file.isEmpty()) {
            String uploadDir = "uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String fileName = username + "_" + file.getOriginalFilename();
            file.transferTo(new File(uploadDir + fileName));
            user.setProfileImage(fileName);
            userRepository.save(user);
        }

        return ResponseEntity.ok(Map.of("message", "Profile updated", "profileImage", user.getProfileImage()));
    }

    // ================= NOTIFICATIONS =================

    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications(Principal principal) {
        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> data = new HashMap<>();
        data.put("autoNotifications", notificationService.getAutoNotifications(user));
        data.put("adminNotifications", notificationService.getAdminNotifications(user));

        return ResponseEntity.ok(data);
    }

    // ================= ANALYTICS =================

    @GetMapping("/analytics")
    public ResponseEntity<?> analytics(Principal principal) {
        String username = principal.getName();

        Map<String, Object> data = new HashMap<>();
        data.put("today", userService.getTodayDataList(username));
        data.put("weekly", userService.getWeeklyData(username));
        data.put("monthly", userService.getMonthlyData(username));

        return ResponseEntity.ok(data);
    }

    // ================= EXPORT =================
    @GetMapping("/report/download")
    public ResponseEntity<byte[]> downloadReport(@RequestParam(defaultValue = "today") String range,
                                                 @RequestParam(required = false) String source,
                                                 Principal principal) {
        String username = principal.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<EnergyData> targetData;
        if (range.equalsIgnoreCase("weekly")) {
            targetData = userService.getWeeklyData(username);
        } else if (range.equalsIgnoreCase("monthly")) {
            targetData = userService.getMonthlyData(username);
        } else {
            targetData = userService.getTodayDataList(username);
        }

        if (source != null && !source.isEmpty() && !source.equalsIgnoreCase("All")) {
            targetData = targetData.stream().filter(d -> source.equalsIgnoreCase(d.getSource())).toList();
        }

        String reportTitle = range + (source != null && !source.equalsIgnoreCase("All") ? " (" + source + ")" : "");
        List<com.repm.backend.entity.Notification> notifications = notificationService.getUserNotifications(user);
        java.io.ByteArrayInputStream bis = pdfReportService.generateUserReport(user, targetData, notifications, reportTitle.toUpperCase());
        byte[] pdfBytes = bis.readAllBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=" + reportTitle.replaceAll("\\s+", "_") + "_performance_report.pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    // ================= MESSAGING =================

    @GetMapping("/notifications/inbox")
    public ResponseEntity<?> getInbox(Principal principal) {
        User user = userRepository.findByUsername(principal.getName()).orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(notificationService.getUserNotifications(user));
    }

    @GetMapping("/notifications/sent")
    public ResponseEntity<?> getSent(Principal principal) {
        return ResponseEntity.ok(notificationService.getSentUserMessages(principal.getName()));
    }

    @PostMapping("/notifications/reply")
    public ResponseEntity<?> reply(@RequestBody Map<String, Object> payload, Principal principal) {
        Long parentId = Long.valueOf(payload.get("parentId").toString());
        String message = payload.get("message").toString();
        com.repm.backend.entity.Notification parent = notificationRepository.findById(parentId).orElseThrow(() -> new RuntimeException("Message not found"));
        
        User admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            admin = userRepository.findAll().stream().filter(u -> u.getRole().equals("ADMIN")).findFirst().orElse(null);
        }
        
        notificationService.createNotification(admin, message, "REPLY", "USER_TO_ADMIN", principal.getName(), parentId);
        return ResponseEntity.ok(Map.of("message", "Reply sent to Admin"));
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("message", "Read"));
    }
}