package com.example.mcp.common.vehicle;

import java.util.Objects;

public final class VehicleStateRequest {
    private final String vehicleId;
    private final int targetTemperature;
    private final boolean startEngine;

    public VehicleStateRequest(String vehicleId, int targetTemperature, boolean startEngine) {
        this.vehicleId = Objects.requireNonNull(vehicleId, "vehicleId must not be null");
        this.targetTemperature = targetTemperature;
        this.startEngine = startEngine;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public int getTargetTemperature() {
        return targetTemperature;
    }

    public boolean shouldStartEngine() {
        return startEngine;
    }

    @Override
    public String toString() {
        return "VehicleStateRequest{" +
                "vehicleId='" + vehicleId + '\'' +
                ", targetTemperature=" + targetTemperature +
                ", startEngine=" + startEngine +
                '}';
    }
}
