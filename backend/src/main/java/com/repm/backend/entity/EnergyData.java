package com.repm.backend.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;

@Entity
@Table(name = "energy_data")
public class EnergyData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_name")
    private String source;

    // ===== INPUTS =====
    private Double energyProduced;
    private Double energyConsumed;
    private Double systemCapacity;
    private Double operatingHours;
    private Double electricityRate;

    // ===== OUTPUTS =====
    private Double efficiency;
    private Double electricityBill;
    private Double netEnergy;
    private Double co2Consumed;
    private Double co2Produced;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDate entryDate;

    @PrePersist
    public void prePersist() {
        this.entryDate = LocalDate.now();
    }

    // ===== GETTERS & SETTERS =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Double getEnergyProduced() { return energyProduced; }
    public void setEnergyProduced(Double energyProduced) { this.energyProduced = energyProduced; }

    public Double getEnergyConsumed() { return energyConsumed; }
    public void setEnergyConsumed(Double energyConsumed) { this.energyConsumed = energyConsumed; }

    public Double getSystemCapacity() { return systemCapacity; }
    public void setSystemCapacity(Double systemCapacity) { this.systemCapacity = systemCapacity; }

    public Double getOperatingHours() { return operatingHours; }
    public void setOperatingHours(Double operatingHours) { this.operatingHours = operatingHours; }

    public Double getElectricityRate() { return electricityRate; }
    public void setElectricityRate(Double electricityRate) { this.electricityRate = electricityRate; }

    public Double getEfficiency() { return efficiency; }
    public void setEfficiency(Double efficiency) { this.efficiency = efficiency; }

    public Double getElectricityBill() { return electricityBill; }
    public void setElectricityBill(Double electricityBill) { this.electricityBill = electricityBill; }

    public Double getNetEnergy() { return netEnergy; }
    public void setNetEnergy(Double netEnergy) { this.netEnergy = netEnergy; }

    public Double getCo2Consumed() { return co2Consumed; }
    public void setCo2Consumed(Double co2Consumed) { this.co2Consumed = co2Consumed; }

    public Double getCo2Produced() { return co2Produced; }
    public void setCo2Produced(Double co2Produced) { this.co2Produced = co2Produced; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }
}