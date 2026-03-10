package com.repm.backend.dto;

public class LeaderboardDTO {

    private String username;
    private double efficiency;

    public LeaderboardDTO(String username, double efficiency) {
        this.username = username;
        this.efficiency = efficiency;
    }

    public String getUsername() {
        return username;
    }

    public double getEfficiency() {
        return efficiency;
    }
}
