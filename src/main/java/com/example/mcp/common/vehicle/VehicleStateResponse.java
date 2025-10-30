package com.example.mcp.common.vehicle;

import java.util.Objects;

public final class VehicleStateResponse {
    private final String vehicleId;
    private final int cabinTemperature;
    private final boolean engineRunning;

    public VehicleStateResponse(String vehicleId, int cabinTemperature, boolean engineRunning) {
        this.vehicleId = Objects.requireNonNull(vehicleId, "vehicleId must not be null");
        this.cabinTemperature = cabinTemperature;
        this.engineRunning = engineRunning;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public int getCabinTemperature() {
        return cabinTemperature;
    }

    public boolean isEngineRunning() {
        return engineRunning;
    }

    @Override
    public String toString() {
        return "VehicleStateResponse{" +
                "vehicleId='" + vehicleId + '\'' +
                ", cabinTemperature=" + cabinTemperature +
                ", engineRunning=" + engineRunning +
                '}';
    }
}
