package com.repm.backend.service;

import com.repm.backend.entity.Notification;
import com.repm.backend.entity.User;
import com.repm.backend.entity.EnergyData;
import com.repm.backend.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // ================= CREATE NOTIFICATION =================

    public void createNotification(User user, String message, String source, String type) {

        Notification n = new Notification();
        n.setUser(user);
        n.setMessage(message);
        n.setSource(source);
        n.setType(type);
        n.setTimestamp(LocalDateTime.now());

        notificationRepository.save(n);
    }

    // ================= BACKWARD COMPATIBILITY =================

    public void sendNotification(User user, String message, String source) {
        createNotification(user, message, source, "AUTO");
    }

    // ================= AUTO NOTIFICATION LOGIC =================

    public void generateAutoNotifications(User user, EnergyData data) {

        String source = data.getSource();

        // Efficiency Notification
        if (data.getEfficiency() >= 80) {
            createNotification(user,
                    "✅ " + source + " efficiency is excellent today.",
                    source, "AUTO");

        } else if (data.getEfficiency() >= 60) {
            createNotification(user,
                    "⚠️ " + source + " efficiency is moderate.",
                    source, "AUTO");

        } else {
            createNotification(user,
                    "❌ " + source + " efficiency is low. Check system performance.",
                    source, "AUTO");
        }

        // Electricity Bill Notification
        if (data.getElectricityBill() > 500) {
            createNotification(user,
                    "❌ Electricity bill is high today (₹" + data.getElectricityBill() + ").",
                    source, "AUTO");
        }

        // Energy Balance
        if (data.getEnergyProduced() > data.getEnergyConsumed()) {
            createNotification(user,
                    "⚡ " + source + " generated surplus energy today.",
                    source, "AUTO");
        } else {
            createNotification(user,
                    "⚠️ " + source + " energy production is lower than consumption.",
                    source, "AUTO");
        }

        // CO2 Environmental Notification
        if (data.getCo2Consumed() > data.getCo2Produced()) {
            createNotification(user,
                    "🌿 Great! Your system saved more CO₂ than it emitted.",
                    source, "AUTO");
        } else {
            createNotification(user,
                    "🌫 CO₂ emission is higher today. Try improving renewable usage.",
                    source, "AUTO");
        }
    }

    // ================= GET NOTIFICATIONS =================

    public List<Notification> getUserNotifications(User user) {
        return notificationRepository.findByUser(user);
    }

    public List<Notification> getAutoNotifications(User user) {
        return notificationRepository.findByUserAndType(user, "AUTO");
    }

    public List<Notification> getAdminNotifications(User user) {
        return notificationRepository.findByUserAndType(user, "ADMIN");
    }
}