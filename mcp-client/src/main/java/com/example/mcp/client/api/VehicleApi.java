package com.example.mcp.client.api;

import com.example.mcp.client.McpClient;
import com.example.mcp.common.StdResponse;
import java.time.OffsetDateTime;

public class VehicleApi {
  private final McpClient client;

  public VehicleApi(McpClient client) {
    this.client = client;
  }

  public StdResponse<VehicleStateResponse> getState(String vehicleId) throws Exception {
    VehicleStateRequest request = new VehicleStateRequest();
    request.setVehicleId(vehicleId);
    return client.invoke("mcp.vehicle.state.get", request, VehicleStateResponse.class);
  }

  public static class VehicleStateRequest {
    private String vehicleId;

    public String getVehicleId() {
      return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
      this.vehicleId = vehicleId;
    }
  }

  public static class VehicleStateResponse {
    private String vehicleId;
    private Double batteryPercentage;
    private Double latitude;
    private Double longitude;
    private OffsetDateTime updatedAt;

    public String getVehicleId() {
      return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
      this.vehicleId = vehicleId;
    }

    public Double getBatteryPercentage() {
      return batteryPercentage;
    }

    public void setBatteryPercentage(Double batteryPercentage) {
      this.batteryPercentage = batteryPercentage;
    }

    public Double getLatitude() {
      return latitude;
    }

    public void setLatitude(Double latitude) {
      this.latitude = latitude;
    }

    public Double getLongitude() {
      return longitude;
    }

    public void setLongitude(Double longitude) {
      this.longitude = longitude;
    }

    public OffsetDateTime getUpdatedAt() {
      return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
      this.updatedAt = updatedAt;
    }
  }
}
