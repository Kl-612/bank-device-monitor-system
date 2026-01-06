package com.kl.service.impl;

import com.kl.entity.DeviceInfo;
import com.kl.mapper.DeviceInfoMapper;
import com.kl.service.DeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
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
    public boolean markDeviceAsFault(Integer id, String faultReason) {
        try {
            // 1. 检查设备是否存在
            DeviceInfo device = getDeviceById(id);
            if (device == null) {
                throw new RuntimeException("设备不存在，ID: " + id);
            }

            // 2. 银行业务规则：检查是否已经是故障状态
            if ("FAULT".equals(device.getStatus())) {
                System.out.println("设备已是故障状态，无需重复标记");
                return false;
            }

            // 3. 更新状态为故障
            boolean success = changeDeviceStatus(id, "FAULT", faultReason);

            if (success) {
                // 4. 这里可以记录到故障表（后续开发）
                System.out.printf("设备标记为故障 [ID: %d, 名称: %s], 原因: %s%n",
                        id, device.getDeviceName(), faultReason);

                // 5. 银行特色：发送通知（模拟）
                sendFaultNotification(device, faultReason);
            }

            return success;
        } catch (Exception e) {
            System.err.println("标记设备故障失败: " + e.getMessage());
            return false;
        }
    }

    // 私有方法：发送故障通知（模拟）
    private void sendFaultNotification(DeviceInfo device, String reason) {
        String message = String.format(
                "【银行设备故障报警】\n设备: %s (%s)\n位置: %s\n状态: %s -> FAULT\n原因: %s\n时间: %s",
                device.getDeviceName(), device.getDeviceId(),
                device.getLocation(), device.getStatus(), reason,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
        );
        System.out.println("发送通知: " + message);
    }

    @Override
    public Map<String, Object> searchDevices(String keyword, String deviceType, String branch) {
        Map<String, Object> result = new HashMap<>();

        try {

            // Java代码过滤
            List<DeviceInfo> allDevices = deviceInfoMapper.selectAll();
            List<DeviceInfo> filteredDevices = filterDevices(allDevices, keyword, deviceType, branch);

            // 构建结果
            result.put("success", true);
            result.put("total", filteredDevices.size());
            result.put("devices", filteredDevices);

            // 搜索条件记录（便于调试）
            Map<String, String> searchParams = new HashMap<>();
            if (keyword != null) searchParams.put("keyword", keyword);
            if (deviceType != null) searchParams.put("deviceType", deviceType);
            if (branch != null) searchParams.put("branch", branch);
            result.put("searchParams", searchParams);

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "搜索失败: " + e.getMessage());
            result.put("total", 0);
            result.put("devices", Collections.emptyList());
        }

        return result;
    }

    // 辅助方法：Java代码过滤
    private List<DeviceInfo> filterDevices(List<DeviceInfo> devices,
                                           String keyword,
                                           String deviceType,
                                           String branch) {
        List<DeviceInfo> filtered = new ArrayList<>();

        for (DeviceInfo device : devices) {
            boolean match = true;

            // 关键词匹配（多个字段）
            if (keyword != null && !keyword.trim().isEmpty()) {
                String lowerKeyword = keyword.toLowerCase().trim();
                match = match && (
                        device.getDeviceName().toLowerCase().contains(lowerKeyword) ||
                                device.getDeviceId().toLowerCase().contains(lowerKeyword) ||
                                device.getLocation().toLowerCase().contains(lowerKeyword) ||
                                (device.getBranch() != null && device.getBranch().toLowerCase().contains(lowerKeyword))
                );
            }

            // 设备类型匹配
            if (deviceType != null && !deviceType.trim().isEmpty()) {
                match = match && deviceType.trim().equalsIgnoreCase(device.getDeviceType());
            }

            // 支行匹配
            if (branch != null && !branch.trim().isEmpty()) {
                match = match && device.getBranch() != null &&
                        device.getBranch().toLowerCase().contains(branch.toLowerCase().trim());
            }

            if (match) {
                filtered.add(device);
            }
        }

        return filtered;
    }

    @Override
    public List<DeviceInfo> getDevicesByBranch(String branch) {
        if (branch == null || branch.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // 支持模糊查询
            return deviceInfoMapper.selectByBranch("%" + branch.trim() + "%");
        } catch (Exception e) {
            System.err.println("按支行查询失败: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getWarrantyAlertDevices() {
        return deviceInfoMapper.getWarrantyAlertDevices();
    }


    @Override
    public Map<String, Object> getFaultAnalysis() {
        return deviceInfoMapper.getFaultAnalysis();
    }

    @Override
    public Map<String, Object> getBranchHealthStats() {
        // 1. 获取原始数据统计
        List<Map<String, Object>> branchStats = deviceInfoMapper.getBranchHealthStats();

        // 2. 计算总体在线率（修复类型转换）
        long totalDevices = 0;
        long totalOnline = 0;

        for (Map<String, Object> stat : branchStats) {
            // 处理 total（BigDecimal -> Long）
            Object totalObj = stat.get("total");
            long total = 0;
            if (totalObj instanceof BigDecimal) {
                total = ((BigDecimal) totalObj).longValue();
            } else if (totalObj instanceof Long) {
                total = (Long) totalObj;
            } else if (totalObj instanceof Integer) {
                total = ((Integer) totalObj).longValue();
            }
            totalDevices += total;

            // 处理 online（BigDecimal -> Long）
            Object onlineObj = stat.get("online");
            long online = 0;
            if (onlineObj instanceof BigDecimal) {
                online = ((BigDecimal) onlineObj).longValue();
            } else if (onlineObj instanceof Long) {
                online = (Long) onlineObj;
            } else if (onlineObj instanceof Integer) {
                online = ((Integer) onlineObj).longValue();
            }
            totalOnline += online;
        }

        // 3. 返回结构化的数据
        Map<String, Object> result = new HashMap<>();
        result.put("branchStats", branchStats);
        result.put("overallOnlineRate", totalDevices > 0 ?
                String.format("%.2f%%", totalOnline * 100.0 / totalDevices) : "0%");
        result.put("timestamp", LocalDateTime.now());

        return result;
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