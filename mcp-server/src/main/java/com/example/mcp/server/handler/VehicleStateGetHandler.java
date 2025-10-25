package com.example.mcp.server.handler;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

@Component
public class VehicleStateGetHandler
    implements ToolHandler<VehicleStateGetHandler.VehicleStateRequest, VehicleStateGetHandler.VehicleStateResponse> {

  @Override
  public String getToolName() {
    return "mcp.vehicle.state.get";
  }

  @Override
  public Class<VehicleStateRequest> getRequestType() {
    return VehicleStateRequest.class;
  }

  @Override
  public StdResponse<VehicleStateResponse> handle(Context context, VehicleStateRequest request) {
    VehicleStateResponse response = new VehicleStateResponse();
    response.setVehicleId(request.getVehicleId());
    response.setBatteryPercentage(82.5);
    response.setLatitude(37.7749);
    response.setLongitude(-122.4194);
    response.setUpdatedAt(OffsetDateTime.now());
    Context.Usage usage = new Context.Usage();
    usage.setLatencyMs(25L);
    context.setUsage(usage);
    return StdResponse.success("VEHICLE_STATE", "Vehicle state retrieved", response);
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
