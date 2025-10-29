package com.example.mcp.server.tool;

import com.example.mcp.common.StdResponse;
import org.springframework.ai.mcp.server.annotation.McpTool;
import org.springframework.stereotype.Component;

/**
 * 演示车控查询工具使用 Spring AI MCP 暴露。
 */
@Component
public class VehicleStateMcpTool {

    @McpTool(
            name = "mcp.vehicle.state.get",
            description = "获取车辆当前运行状态",
            inputSchema = "schema/vehicle-state-request.json",
            outputSchema = "schema/vehicle-state-response.json")
    public StdResponse<VehicleStateResponse> getState(VehicleStateRequest request) {
        VehicleStateResponse response = new VehicleStateResponse();
        response.setVin(request.getVin());
        response.setBatteryLevel(0.68);
        response.setCharging("fast".equalsIgnoreCase(request.getMode()));
        response.setLocation("Shanghai, CN");
        return StdResponse.success("VEHICLE_STATE", "车辆状态查询成功", response);
    }

    public static class VehicleStateRequest {
        private String vin;
        private String mode;

        public String getVin() {
            return vin;
        }

        public void setVin(String vin) {
            this.vin = vin;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }
    }

    public static class VehicleStateResponse {
        private String vin;
        private double batteryLevel;
        private boolean charging;
        private String location;

        public String getVin() {
            return vin;
        }

        public void setVin(String vin) {
            this.vin = vin;
        }

        public double getBatteryLevel() {
            return batteryLevel;
        }

        public void setBatteryLevel(double batteryLevel) {
            this.batteryLevel = batteryLevel;
        }

        public boolean isCharging() {
            return charging;
        }

        public void setCharging(boolean charging) {
            this.charging = charging;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }
    }
}
