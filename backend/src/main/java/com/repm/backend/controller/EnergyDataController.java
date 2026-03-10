package com.repm.backend.controller;

import com.repm.backend.entity.EnergyData;
import com.repm.backend.service.EnergyDataService;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/energy")
@CrossOrigin(origins = "*")
public class EnergyDataController {

    private final EnergyDataService service;

    public EnergyDataController(EnergyDataService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<EnergyData> save(@RequestBody EnergyData data) {

        if (data == null) {
            return ResponseEntity.badRequest().build();
        }

        EnergyData saved = service.save(data);

        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<EnergyData>> getAll() {

        List<EnergyData> list = service.getAll();

        return ResponseEntity.ok(list);
    }
}
