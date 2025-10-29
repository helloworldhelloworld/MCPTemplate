package com.example.mcp.server.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.mcp.common.StdResponse;
import org.junit.jupiter.api.Test;

class VehicleStateMcpToolTest {

    @Test
    void returnsVehicleSnapshot() {
        VehicleStateMcpTool tool = new VehicleStateMcpTool();
        VehicleStateMcpTool.VehicleStateRequest request = new VehicleStateMcpTool.VehicleStateRequest();
        request.setVin("VIN123456");
        request.setMode("fast");

        StdResponse<VehicleStateMcpTool.VehicleStateResponse> response = tool.getState(request);

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getVin()).isEqualTo("VIN123456");
        assertThat(response.getData().isCharging()).isTrue();
    }
}
