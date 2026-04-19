package com.repm.backend.controller;

import com.repm.backend.repository.UserRepository;
import com.repm.backend.service.AdminService;
import com.repm.backend.service.NotificationService;
import com.repm.backend.service.PdfReportService;
import com.repm.backend.entity.User;
import com.repm.backend.entity.EnergyData;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminService adminService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final PdfReportService pdfReportService;
    private final com.repm.backend.repository.NotificationRepository notificationRepository;

    public AdminController(AdminService adminService,
                           NotificationService notificationService,
                           UserRepository userRepository,
                           PdfReportService pdfReportService,
                           com.repm.backend.repository.NotificationRepository notificationRepository) {
        this.adminService = adminService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.pdfReportService = pdfReportService;
        this.notificationRepository = notificationRepository;
    }

    // ================= DASHBOARD =================

    @GetMapping("/dashboard")
    public ResponseEntity<?> adminDashboard(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("username", principal.getName());
        data.put("avgEfficiency", adminService.getAverageEfficiency());
        data.put("avgCO2Consumed", adminService.getAverageCO2Consumed());
        data.put("avgCO2Produced", adminService.getAverageCO2Produced());
        data.put("totalElectricityBill", adminService.getTotalElectricityBill());

        data.put("solarEff", adminService.getAverageEfficiencyBySource("Solar"));
        data.put("solarCO2", adminService.getAverageCO2ConsumedBySource("Solar"));
        data.put("solarBill", adminService.getTotalElectricityBillBySource("Solar"));

        data.put("windEff", adminService.getAverageEfficiencyBySource("Wind"));
        data.put("windCO2", adminService.getAverageCO2ConsumedBySource("Wind"));
        data.put("windBill", adminService.getTotalElectricityBillBySource("Wind"));

        data.put("hydroEff", adminService.getAverageEfficiencyBySource("Hydro"));
        data.put("hydroCO2", adminService.getAverageCO2ConsumedBySource("Hydro"));
        data.put("hydroBill", adminService.getTotalElectricityBillBySource("Hydro"));

        return ResponseEntity.ok(data);
    }

    // ================= USERS =================

    @GetMapping("/users")
    public ResponseEntity<?> manageUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    // ================= LEADERBOARD =================

    @GetMapping("/leaderboard")
    public ResponseEntity<?> leaderboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("solarLeaders", adminService.getTopUsersBySource("Solar"));
        data.put("windLeaders", adminService.getTopUsersBySource("Wind"));
        data.put("hydroLeaders", adminService.getTopUsersBySource("Hydro"));
        return ResponseEntity.ok(data);
    }

    // ================= SEND NOTIFICATION =================

    @PostMapping("/notify")
    public ResponseEntity<?> sendNotification(@RequestBody Map<String, Object> payload, Principal principal) {
        Long userId = Long.valueOf(payload.get("userId").toString());
        String message = payload.get("message").toString();
        String source = payload.get("source").toString();

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        notificationService.createNotification(user, message, source, "ADMIN_TO_USER", principal.getName(), null);

        return ResponseEntity.ok(Map.of("message", "Notification sent"));
    }

    @GetMapping("/notifications/sent")
    public ResponseEntity<?> getSentNotifications(Principal principal) {
        return ResponseEntity.ok(notificationService.getSentMessages(principal.getName()));
    }

    @PostMapping("/notifications/reply")
    public ResponseEntity<?> replyToMessage(@RequestBody Map<String, Object> payload, Principal principal) {
        Long parentId = Long.valueOf(payload.get("parentId").toString());
        String message = payload.get("message").toString();
        com.repm.backend.entity.Notification parent = notificationRepository.findById(parentId).orElseThrow(() -> new RuntimeException("Message not found"));
        
        // If the admin is replying to a message they SENT, the recipient is parent.user
        // If the admin is replying to a message they RECEIVED, the recipient is found by senderUsername
        User recipient;
        if (parent.getSenderUsername() != null && parent.getSenderUsername().equals(principal.getName())) {
            recipient = parent.getUser();
        } else {
            recipient = userRepository.findByUsername(parent.getSenderUsername()).orElseThrow(() -> new RuntimeException("Recipient not found"));
        }
        
        notificationService.createNotification(recipient, message, "REPLY", "ADMIN_TO_USER", principal.getName(), parentId);
        return ResponseEntity.ok(Map.of("message", "Reply sent"));
    }

    // ================= ALERT =================

    @PostMapping("/send-alert")
    public ResponseEntity<?> sendAlert(@RequestBody Map<String, Object> payload) {
        Long userId = Long.valueOf(payload.get("userId").toString());
        String message = payload.get("message").toString();
        String source = payload.get("source").toString();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        notificationService.createNotification(user, message, source, "ADMIN");

        return ResponseEntity.ok(Map.of("message", "Alert sent"));
    }

    // ================= NOTIFICATIONS =================

    @GetMapping("/notifications/all")
    public ResponseEntity<?> getAllNotifications() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    @GetMapping("/user/{id}/notifications")
    public ResponseEntity<?> getUserNotifications(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(notificationService.getUserNotifications(user));
    }

    @GetMapping("/user/{id}/report")
    public ResponseEntity<byte[]> getUserReport(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "monthly") String range,
                                                 @RequestParam(required = false) String source) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        List<com.repm.backend.entity.EnergyData> data;

        if (source != null && !source.isEmpty()) {
            data = adminService.getUserEnergyDataBySource(id, source);
        } else {
            data = adminService.getUserEnergyData(id);
        }

        // Filter by range
        final String finalRange = range;
        java.time.LocalDate now = java.time.LocalDate.now();
        data = data.stream().filter(d -> {
            if ("today".equals(finalRange)) return d.getEntryDate().equals(now);
            if ("weekly".equals(finalRange)) return d.getEntryDate().isAfter(now.minusWeeks(1));
            return d.getEntryDate().isAfter(now.minusMonths(1));
        }).collect(java.util.stream.Collectors.toList());

        List<com.repm.backend.entity.Notification> notifications = notificationRepository.findByUser(user);

        java.io.ByteArrayInputStream bis = pdfReportService.generateUserReport(user, data, notifications, range.toUpperCase());
        byte[] pdfBytes = bis.readAllBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=" + user.getUsername() + "_report.pdf");
        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(pdfBytes);
    }

    // ================= ANALYTICS =================

    @GetMapping("/analytics")
    public ResponseEntity<?> analytics() {
        return ResponseEntity.ok(adminService.getAllEnergyData());
    }

    // ================= UPDATE DATA =================

    @PostMapping("/update-data")
    public ResponseEntity<?> updateData(@RequestBody Map<String, Object> payload) {
        Long id = Long.valueOf(payload.get("id").toString());
        Double produced = Double.valueOf(payload.get("produced").toString());
        Double consumed = Double.valueOf(payload.get("consumed").toString());

        adminService.updateEnergyData(id, produced, consumed);

        return ResponseEntity.ok(Map.of("message", "Data updated"));
    }

    // ================= SETTINGS =================

    @GetMapping("/settings")
    public ResponseEntity<?> adminSettings(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        return ResponseEntity.ok(Map.of("username", principal.getName()));
    }

    // ================= EXPORT =================
    @GetMapping("/report/download")
    public ResponseEntity<byte[]> downloadAdminReport(@RequestParam(defaultValue = "All Time") String range,
                                                      @RequestParam(required = false) String source) {
        List<User> users = userRepository.findAll();
        List<EnergyData> allData = adminService.getAllEnergyData();

        java.time.LocalDate now = java.time.LocalDate.now();
        if ("today".equalsIgnoreCase(range)) {
            allData = allData.stream().filter(d -> d.getEntryDate() != null && d.getEntryDate().isEqual(now)).toList();
        } else if ("weekly".equalsIgnoreCase(range)) {
            allData = allData.stream().filter(d -> d.getEntryDate() != null && !d.getEntryDate().isBefore(now.minusDays(7))).toList();
        } else if ("monthly".equalsIgnoreCase(range)) {
            allData = allData.stream().filter(d -> d.getEntryDate() != null && !d.getEntryDate().isBefore(now.minusDays(30))).toList();
        }

        if (source != null && !source.isEmpty() && !source.equalsIgnoreCase("All")) {
            allData = allData.stream().filter(d -> source.equalsIgnoreCase(d.getSource())).toList();
        }

        String reportTitle = range + (source != null && !source.equalsIgnoreCase("All") ? " (" + source + ")" : "");
        java.io.ByteArrayInputStream bis = pdfReportService.generateAdminReport(users, allData, reportTitle);
        byte[] pdfBytes = bis.readAllBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=admin_" + reportTitle.replaceAll("\\s+", "_") + "_report.pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
