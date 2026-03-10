package com.repm.backend.repository;

import com.repm.backend.entity.EnergyData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EnergyDataRepository extends JpaRepository<EnergyData, Long> {

    // Find all entries by date
    List<EnergyData> findByEntryDate(LocalDate entryDate);
    List<EnergyData> findByUser_Id(Long userId);
    // Find all entries by user ID and date
    List<EnergyData> findByUser_IdAndEntryDate(Long userId, LocalDate entryDate);
    List<EnergyData> findBySource(String source);
    List<EnergyData> findBySourceAndEntryDate(String source, LocalDate entryDate);
    List<EnergyData> findByUser_IdAndSource(Long userId, String source);
    List<EnergyData> findByUser_IdAndEntryDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    List<EnergyData> findByUser_IdAndSourceIn(Long userId, List<String> sources);
}
