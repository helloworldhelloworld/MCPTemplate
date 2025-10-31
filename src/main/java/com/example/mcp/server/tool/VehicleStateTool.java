package com.example.mcp.server.tool;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;
import com.example.mcp.common.vehicle.VehicleStateRequest;
import com.example.mcp.common.vehicle.VehicleStateResponse;
import com.example.mcp.framework.api.ToolHandler;
import java.util.HashMap;
import java.util.Map;

public final class VehicleStateTool implements ToolHandler<VehicleStateRequest, VehicleStateResponse> {

    private final Map<String, VehicleStateResponse> stateStore = new HashMap<>();

    @Override
    public StdResponse<VehicleStateResponse> handle(Context context, VehicleStateRequest input) {
        VehicleStateResponse current = stateStore.getOrDefault(input.getVehicleId(),
                new VehicleStateResponse(input.getVehicleId(), 24, false));
        boolean engineRunning = input.shouldStartEngine() || current.isEngineRunning();
        int adjustedTemperature = Math.max(16, Math.min(30, input.getTargetTemperature()));
        VehicleStateResponse updated = new VehicleStateResponse(input.getVehicleId(), adjustedTemperature, engineRunning);
        stateStore.put(input.getVehicleId(), updated);
        return StdResponse.success("vehicle_state", "已同步车控状态", updated);
    }
}
