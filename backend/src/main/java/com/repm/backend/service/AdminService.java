package com.repm.backend.service;

import com.repm.backend.dto.LeaderboardDTO;
import com.repm.backend.entity.EnergyData;
import com.repm.backend.entity.User;
import com.repm.backend.repository.EnergyDataRepository;
import com.repm.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AdminService {

    private final EnergyDataRepository energyDataRepository;
    private final UserRepository userRepository;

    public AdminService(EnergyDataRepository energyDataRepository,
                        UserRepository userRepository) {
        this.energyDataRepository = energyDataRepository;
        this.userRepository = userRepository;
    }

    // ================= USERS =================

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ================= OVERALL ANALYTICS =================

    public double getAverageEfficiency() {

        List<EnergyData> data = energyDataRepository.findAll();

        if (data.isEmpty()) return 0;

        return data.stream()
                .mapToDouble(d -> d.getEfficiency() != null ? d.getEfficiency() : 0)
                .average()
                .orElse(0);
    }

    public double getAverageCO2Consumed() {

        List<EnergyData> data = energyDataRepository.findAll();

        if (data.isEmpty()) return 0;

        return data.stream()
                .mapToDouble(d -> d.getCo2Consumed() != null ? d.getCo2Consumed() : 0)
                .average()
                .orElse(0);
    }

    public double getAverageCO2Produced() {

        List<EnergyData> data = energyDataRepository.findAll();

        if (data.isEmpty()) return 0;

        return data.stream()
                .mapToDouble(d -> d.getCo2Produced() != null ? d.getCo2Produced() : 0)
                .average()
                .orElse(0);
    }

    public double getTotalElectricityBill() {

        List<EnergyData> data = energyDataRepository.findAll();

        return data.stream()
                .mapToDouble(d -> d.getElectricityBill() != null ? d.getElectricityBill() : 0)
                .sum();
    }

    // ================= SOURCE BASED ANALYTICS =================

    public double getAverageEfficiencyBySource(String source) {

        List<EnergyData> data = energyDataRepository.findBySource(source);

        if (data.isEmpty()) return 0;

        return data.stream()
                .mapToDouble(d -> d.getEfficiency() != null ? d.getEfficiency() : 0)
                .average()
                .orElse(0);
    }

    public double getAverageCO2ConsumedBySource(String source) {

        List<EnergyData> data = energyDataRepository.findBySource(source);

        if (data.isEmpty()) return 0;

        return data.stream()
                .mapToDouble(d -> d.getCo2Consumed() != null ? d.getCo2Consumed() : 0)
                .average()
                .orElse(0);
    }

    public double getAverageCO2ProducedBySource(String source) {

        List<EnergyData> data = energyDataRepository.findBySource(source);

        if (data.isEmpty()) return 0;

        return data.stream()
                .mapToDouble(d -> d.getCo2Produced() != null ? d.getCo2Produced() : 0)
                .average()
                .orElse(0);
    }

    public double getTotalElectricityBillBySource(String source) {

        List<EnergyData> data = energyDataRepository.findBySource(source);

        return data.stream()
                .mapToDouble(d -> d.getElectricityBill() != null ? d.getElectricityBill() : 0)
                .sum();
    }

    // ================= GLOBAL LEADERBOARD =================

    public List<LeaderboardDTO> getLeaderboard() {

        Map<String, Double> userEfficiencyMap = new HashMap<>();

        List<EnergyData> dataList = energyDataRepository.findAll();

        for (EnergyData data : dataList) {

            if (data.getUser() == null) continue;

            String username = data.getUser().getUsername();

            double efficiency =
                    data.getEfficiency() != null ? data.getEfficiency() : 0;

            userEfficiencyMap.put(
                    username,
                    userEfficiencyMap.getOrDefault(username, 0.0) + efficiency
            );
        }

        return userEfficiencyMap.entrySet()
                .stream()
                .map(e -> new LeaderboardDTO(e.getKey(), e.getValue()))
                .sorted((a, b) ->
                        Double.compare(b.getEfficiency(), a.getEfficiency()))
                .toList();
    }

    // ================= TOP USERS BY SOURCE =================

    public List<LeaderboardDTO> getTopUsersBySource(String source) {

        List<EnergyData> data = energyDataRepository.findBySource(source);

        Map<String, Double> userEfficiencyMap = new HashMap<>();

        for (EnergyData d : data) {

            if (d.getUser() == null) continue;

            String username = d.getUser().getUsername();

            double efficiency =
                    d.getEfficiency() != null ? d.getEfficiency() : 0;

            userEfficiencyMap.put(
                    username,
                    userEfficiencyMap.getOrDefault(username, 0.0) + efficiency
            );
        }

        return userEfficiencyMap.entrySet()
                .stream()
                .map(e -> new LeaderboardDTO(e.getKey(), e.getValue()))
                .sorted((a, b) ->
                        Double.compare(b.getEfficiency(), a.getEfficiency()))
                .limit(5)
                .toList();
    }

    // ================= ENERGY DATA MANAGEMENT =================

    public List<EnergyData> getAllEnergyData() {
        return energyDataRepository.findAll();
    }

    public void updateEnergyData(Long id,
                                 double produced,
                                 double consumed) {

        EnergyData data = energyDataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Energy data not found"));

        data.setEnergyProduced(produced);
        data.setEnergyConsumed(consumed);

        // Recalculate efficiency
        double efficiency =
                (consumed == 0) ? 0 : (produced / consumed) * 100;

        data.setEfficiency(efficiency);

        energyDataRepository.save(data);
    }

    public List<EnergyData> getUserEnergyData(Long userId) {
        return energyDataRepository.findByUser_Id(userId);
    }

    public List<EnergyData> getUserEnergyDataBySource(Long userId, String source) {
        return energyDataRepository.findByUser_IdAndSource(userId, source);
    }
}