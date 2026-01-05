package com.kl.controller;

import com.kl.entity.DeviceInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    @Autowired
    private com.kl.service.DeviceService deviceService;  // 注入Service

    // GET 所有设备
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllDevices() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<DeviceInfo> devices = deviceService.getAllDevices();
            response.put("success", true);
            response.put("message", "查询成功");
            response.put("total", devices.size());
            response.put("data", devices);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // GET 按状态查询
    @GetMapping("/status/{status}")
    public ResponseEntity<Map<String, Object>> getDevicesByStatus(@PathVariable String status) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<DeviceInfo> devices = deviceService.getDevicesByStatus(status);
            response.put("success", true);
            response.put("message", "查询成功");
            response.put("total", devices.size());
            response.put("data", devices);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // POST 添加设备
    @PostMapping
    public ResponseEntity<Map<String, Object>> addDevice(@RequestBody DeviceInfo device) {
        Map<String, Object> response = new HashMap<>();
        try {
            DeviceInfo savedDevice = deviceService.addDevice(device);
            response.put("success", true);
            response.put("message", "设备添加成功，等待验收上线");
            response.put("data", savedDevice);
            return ResponseEntity.status(201).body(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", "参数错误: " + e.getMessage());
            return ResponseEntity.status(400).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "添加失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // PUT 更新设备
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateDevice(
            @PathVariable Integer id,
            @RequestBody DeviceInfo device) {

        Map<String, Object> response = new HashMap<>();
        try {
            DeviceInfo updatedDevice = deviceService.updateDevice(id, device);
            response.put("success", true);
            response.put("message", "设备更新成功");
            response.put("data", updatedDevice);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", "参数错误: " + e.getMessage());
            return ResponseEntity.status(400).body(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(400).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "更新失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // DELETE 删除设备
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteDevice(@PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();
        try {
            deviceService.deleteDevice(id);
            response.put("success", true);
            response.put("message", "设备删除成功");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(400).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "删除失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // 设备统计接口
    @GetMapping("/stats/summary")
    public ResponseEntity<Map<String, Object>> getDeviceSummary() {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> stats = deviceService.getDeviceStatistics();
            response.put("success", true);
            response.put("message", "统计信息获取成功");
            response.put("data", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "统计失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // 设备状态变更接口
    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateDeviceStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {

        Map<String, Object> response = new HashMap<>();
        try {
            String newStatus = request.get("status");
            String changeReason = request.get("reason");

            if (newStatus == null || newStatus.isEmpty()) {
                response.put("success", false);
                response.put("message", "状态不能为空");
                return ResponseEntity.status(400).body(response);
            }

            boolean success = deviceService.changeDeviceStatus(id, newStatus, changeReason);

            if (success) {
                response.put("success", true);
                response.put("message", "设备状态更新成功");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "状态更新失败");
                return ResponseEntity.status(500).body(response);
            }
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(400).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "状态更新失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // 设备搜索接口
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchDevices(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String branch) {

        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = deviceService.searchDevices(keyword, deviceType, branch);
            response.put("success", true);
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "搜索失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // 标记设备故障
    @PostMapping("/{id}/mark-fault")
    public ResponseEntity<Map<String, Object>> markDeviceAsFault(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {

        Map<String, Object> response = new HashMap<>();
        try {
            String faultReason = request.get("reason");
            if (faultReason == null || faultReason.isEmpty()) {
                throw new IllegalArgumentException("必须提供故障原因");
            }

            boolean success = deviceService.markDeviceAsFault(id, faultReason);

            response.put("success", success);
            response.put("message", success ? "设备已标记为故障" : "标记失败");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(400).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "操作失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // 保修预警设备
    @GetMapping("/stats/warranty-alert")
    public ResponseEntity<List<Map<String, Object>>> getWarrantyAlertDevices() {
        return ResponseEntity.ok(deviceService.getWarrantyAlertDevices());
    }

    // 故障分析报告
    @GetMapping("/stats/fault-analysis")
    public ResponseEntity<Map<String, Object>> getFaultAnalysis() {
        return ResponseEntity.ok(deviceService.getFaultAnalysis());
    }
}