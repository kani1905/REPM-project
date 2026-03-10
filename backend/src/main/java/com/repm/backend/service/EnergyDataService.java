package com.repm.backend.service;

import com.repm.backend.entity.EnergyData;
import com.repm.backend.repository.EnergyDataRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnergyDataService {

    private final EnergyDataRepository repository;

    public EnergyDataService(EnergyDataRepository repository) {
        this.repository = repository;
    }

    public EnergyData save(EnergyData data) {
        return repository.save(data);
    }

    public List<EnergyData> getAll() {
        return repository.findAll();
    }

    public List<EnergyData> getByUser(Long userId) {
        return repository.findByUser_Id(userId);
    }
}