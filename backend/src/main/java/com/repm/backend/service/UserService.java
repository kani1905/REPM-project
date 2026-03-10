package com.repm.backend.service;

import com.repm.backend.entity.EnergyData;
import com.repm.backend.entity.User;
import com.repm.backend.repository.EnergyDataRepository;
import com.repm.backend.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final EnergyDataRepository energyDataRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       EnergyDataRepository energyDataRepository,
                       NotificationService notificationService,
                       PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.energyDataRepository = energyDataRepository;
        this.notificationService = notificationService;
        this.passwordEncoder = passwordEncoder;
    }

    // ================= TODAY DATA =================

    public List<EnergyData> getTodayDataList(String username) {

        Optional<User> userOpt = userRepository.findByUsername(username);
        if(userOpt.isEmpty()) return List.of();

        User user = userOpt.get();
        LocalDate today = LocalDate.now();

        List<EnergyData> existing =
                new ArrayList<>(energyDataRepository
                        .findByUser_IdAndEntryDate(user.getId(), today));

        List<String> sources = List.of("Solar","Wind","Hydro");

        for(String src : sources){

            boolean exists = existing.stream()
                    .anyMatch(e -> e.getSource().equalsIgnoreCase(src));

            if(!exists){
                EnergyData empty = new EnergyData();
                empty.setUser(user);
                empty.setSource(src);
                empty.setEnergyProduced(0.0);
                empty.setEnergyConsumed(0.0);
                empty.setEfficiency(0.0);
                empty.setCo2Consumed(0.0);
                empty.setCo2Produced(0.0);
                empty.setElectricityBill(0.0);
                empty.setEntryDate(today);

                existing.add(empty);
            }
        }

        return existing;
    }

    // ================= WEEKLY DATA =================

    public List<EnergyData> getWeeklyData(String username) {

        Optional<User> userOpt = userRepository.findByUsername(username);
        if(userOpt.isEmpty()) return List.of();

        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);

        return energyDataRepository.findByUser_IdAndEntryDateBetween(
                userOpt.get().getId(), weekAgo, today);
    }

    // ================= MONTHLY DATA =================

    public List<EnergyData> getMonthlyData(String username) {

        Optional<User> userOpt = userRepository.findByUsername(username);
        if(userOpt.isEmpty()) return List.of();

        LocalDate today = LocalDate.now();
        LocalDate start = today.withDayOfMonth(1);

        return energyDataRepository.findByUser_IdAndEntryDateBetween(
                userOpt.get().getId(), start, today);
    }

    // ================= SAVE DATA =================

    public EnergyData saveTodayData(String username,
                                    String source,
                                    double energyProduced,
                                    double energyConsumed,
                                    double systemCapacity,
                                    double operatingHours,
                                    double electricityRate) {

        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) return null;

        User user = optionalUser.get();
        LocalDate today = LocalDate.now();

        EnergyData data = energyDataRepository
                .findByUser_IdAndEntryDate(user.getId(), today)
                .stream()
                .filter(d -> d.getSource().equalsIgnoreCase(source))
                .findFirst()
                .orElse(new EnergyData());

        data.setUser(user);
        data.setSource(source);

        data.setEnergyProduced(energyProduced);
        data.setEnergyConsumed(energyConsumed);
        data.setSystemCapacity(systemCapacity);
        data.setOperatingHours(operatingHours);
        data.setElectricityRate(electricityRate);

        // ===== CALCULATIONS =====

        double efficiency =
                (systemCapacity * operatingHours == 0) ? 0 :
                (energyProduced / (systemCapacity * operatingHours)) * 100;

        double bill = energyConsumed * electricityRate;

        double netEnergy = energyProduced - energyConsumed;

        double co2Produced = energyConsumed * 0.82;

        double co2Saved = energyProduced * 0.82;

        data.setEfficiency(efficiency);
        data.setElectricityBill(bill);
        data.setNetEnergy(netEnergy);
        data.setCo2Produced(co2Produced);
        data.setCo2Consumed(co2Saved);

        data.setEntryDate(today);

        EnergyData savedData = energyDataRepository.save(data);

        // ===== AUTO NOTIFICATIONS =====
        notificationService.generateAutoNotifications(user, savedData);

        return savedData;
    }

    // ================= SETTINGS =================

    public void updateUserSettings(String username,
                                   String email,
                                   String password,
                                   String theme) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setEmail(email);

        if (password != null && !password.isBlank()) {
            user.setPassword(passwordEncoder.encode(password));
        }

        if (theme != null) {
            user.setTheme(theme);
        }

        userRepository.save(user);
    }

    // ================= CHANGE PASSWORD =================

    public void updatePassword(String username, String newPassword) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}