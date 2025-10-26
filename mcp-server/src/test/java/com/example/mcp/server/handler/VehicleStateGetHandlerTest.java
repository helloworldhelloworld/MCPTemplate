package com.example.mcp.server.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class VehicleStateGetHandlerTest {

  private final VehicleStateGetHandler handler = new VehicleStateGetHandler();

  @Test
  void handleProvidesVehicleSnapshot() {
    Context context = new Context();
    VehicleStateGetHandler.VehicleStateRequest request = new VehicleStateGetHandler.VehicleStateRequest();
    request.setVehicleId("vehicle-1");

    StdResponse<VehicleStateGetHandler.VehicleStateResponse> response =
        handler.handle(context, request);

    assertEquals("VEHICLE_STATE", response.getCode());
    assertEquals("Vehicle state retrieved", response.getMessage());
    VehicleStateGetHandler.VehicleStateResponse data = response.getData();
    assertNotNull(data);
    assertEquals("vehicle-1", data.getVehicleId());
    assertEquals(82.5, data.getBatteryPercentage());
    assertEquals(37.7749, data.getLatitude());
    assertEquals(-122.4194, data.getLongitude());
    OffsetDateTime updatedAt = data.getUpdatedAt();
    assertNotNull(updatedAt);
    assertNotNull(context.getUsage());
    assertEquals(25L, context.getUsage().getLatencyMs());
  }
}
