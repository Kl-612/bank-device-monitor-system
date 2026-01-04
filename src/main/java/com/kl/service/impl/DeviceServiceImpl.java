package com.kl.service.impl;

import com.kl.entity.DeviceInfo;
import com.kl.mapper.DeviceInfoMapper;
import com.kl.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class DeviceServiceImpl implements DeviceService {

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    @Override
    public List<DeviceInfo> getAllDevices() {
        return deviceInfoMapper.selectAll();
    }

    @Override
    public List<DeviceInfo> getDevicesByStatus(String status) {
        if (status == null || status.isEmpty()) {
            return Collections.emptyList();
        }
        return deviceInfoMapper.selectByStatus(status.toUpperCase());
    }

    @Override
    public DeviceInfo getDeviceById(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("设备ID无效");
        }
        return deviceInfoMapper.selectById(id);
    }

    @Override
    public DeviceInfo getDeviceByDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isEmpty()) {
            throw new IllegalArgumentException("设备唯一标识不能为空");
        }
        return deviceInfoMapper.selectByDeviceId(deviceId);
    }

    @Override
    public DeviceInfo addDevice(DeviceInfo device) {
        // 银行业务逻辑验证
        validateDeviceForAdd(device);

        // 设置默认值
        if (device.getStatus() == null || device.getStatus().isEmpty()) {
            device.setStatus("OFFLINE");
        }

        device.setStatus(device.getStatus().toUpperCase());

        // 检查设备是否已存在
        DeviceInfo existing = deviceInfoMapper.selectByDeviceId(device.getDeviceId());
        if (existing != null) {
            throw new RuntimeException("设备已存在，设备ID: " + device.getDeviceId());
        }

        // 插入数据库
        int result = deviceInfoMapper.insert(device);
        if (result <= 0) {
            throw new RuntimeException("设备添加失败");
        }

        return device;
    }

    @Override
    public DeviceInfo updateDevice(Integer id, DeviceInfo device) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("设备ID无效");
        }

        DeviceInfo existingDevice = deviceInfoMapper.selectById(id);
        if (existingDevice == null) {
            throw new RuntimeException("设备不存在，ID: " + id);
        }

        // 银行业务规则：deviceId不允许修改
        if (device.getDeviceId() != null &&
                !device.getDeviceId().equals(existingDevice.getDeviceId())) {
            throw new RuntimeException("设备唯一标识(deviceId)不允许修改");
        }

        // 设置ID
        device.setId(id);

        // 执行更新
        int result = deviceInfoMapper.update(device);
        if (result <= 0) {
            throw new RuntimeException("设备更新失败");
        }

        // 返回更新后的设备
        return deviceInfoMapper.selectById(id);
    }

    @Override
    public void deleteDevice(Integer id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("设备ID无效");
        }

        DeviceInfo device = deviceInfoMapper.selectById(id);
        if (device == null) {
            throw new RuntimeException("设备不存在，ID: " + id);
        }

        // 银行业务规则：在线设备不能删除
        if ("ONLINE".equals(device.getStatus())) {
            throw new RuntimeException("在线设备不能直接删除，请先将其下线");
        }

        int result = deviceInfoMapper.deleteById(id);
        if (result <= 0) {
            throw new RuntimeException("设备删除失败");
        }
    }

    @Override
    public boolean changeDeviceStatus(Integer id, String newStatus, String changeReason) {
        DeviceInfo device = getDeviceById(id);

        List<String> validStatus = Arrays.asList("ONLINE", "OFFLINE", "FAULT", "MAINTENANCE", "DECOMMISSIONED");
        if (!validStatus.contains(newStatus.toUpperCase())) {
            throw new IllegalArgumentException("无效的设备状态");
        }

        // 记录状态变更原因（银行审计要求）
        System.out.printf("设备状态变更 [设备ID: %s]: %s -> %s, 原因: %s%n",
                device.getDeviceId(), device.getStatus(), newStatus, changeReason);

        // 更新状态
        int result = deviceInfoMapper.updateStatus(id, newStatus.toUpperCase());
        return result > 0;
    }

    @Override
    public boolean markDeviceAsFault(Integer id, String faultReason) {
        return false;
    }

    @Override
    public boolean recoverDeviceFromFault(Integer id, String solution, String maintenancePerson) {
        return false;
    }

    @Override
    public Map<String, Object> getDeviceStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 设备总数
        int totalCount = deviceInfoMapper.countAll();
        stats.put("totalDevices", totalCount);

        // 按状态统计
        List<Map<String, Object>> statusStats = deviceInfoMapper.countByStatus();
        stats.put("statusDistribution", statusStats);

        // 计算在线率
        long onlineCount = statusStats.stream()
                .filter(item -> "ONLINE".equals(item.get("status")))
                .mapToLong(item -> Long.parseLong(item.get("count").toString()))
                .findFirst()
                .orElse(0L);

        double onlineRate = totalCount > 0 ? (onlineCount * 100.0 / totalCount) : 0;
        stats.put("onlineRate", String.format("%.2f%%", onlineRate));

        // 最近更新时间
        stats.put("lastUpdateTime", new Date());

        return stats;
    }

    @Override
    public Map<String, Object> searchDevices(String keyword, String deviceType, String branch) {
        return Map.of();
    }


    @Override
    public List<DeviceInfo> getDevicesNearWarrantyExpiry() {
        return Collections.emptyList();
    }

    @Override
    public List<DeviceInfo> getDevicesByBranch(String branch) {
        if (branch == null || branch.isEmpty()) {
            return Collections.emptyList();
        }
        return deviceInfoMapper.selectByBranch(branch);
    }

    // 私有方法：添加设备时的验证
    private void validateDeviceForAdd(DeviceInfo device) {
        if (device.getDeviceId() == null || device.getDeviceId().isEmpty()) {
            throw new IllegalArgumentException("设备唯一标识(deviceId)不能为空");
        }
        if (device.getDeviceName() == null || device.getDeviceName().isEmpty()) {
            throw new IllegalArgumentException("设备名称不能为空");
        }
        if (device.getDeviceType() == null || device.getDeviceType().isEmpty()) {
            throw new IllegalArgumentException("设备类型不能为空");
        }
        if (device.getLocation() == null || device.getLocation().isEmpty()) {
            throw new IllegalArgumentException("设备位置不能为空");
        }
    }
}