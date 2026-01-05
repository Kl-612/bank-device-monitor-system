package com.kl.service;

import com.kl.entity.DeviceInfo;

import java.util.List;
import java.util.Map;

public interface DeviceService {

    // 查询相关
    List<DeviceInfo> getAllDevices();
    List<DeviceInfo> getDevicesByStatus(String status);
    DeviceInfo getDeviceById(Integer id);
    DeviceInfo getDeviceByDeviceId(String deviceId);

    // 设备管理
    DeviceInfo addDevice(DeviceInfo device);
    DeviceInfo updateDevice(Integer id, DeviceInfo device);
    void deleteDevice(Integer id);

    // 银行业务逻辑
    boolean changeDeviceStatus(Integer id, String newStatus, String changeReason);
    boolean markDeviceAsFault(Integer id, String faultReason);

    // 统计分析
    Map<String, Object> getDeviceStatistics();
    Map<String, Object> searchDevices(String keyword, String deviceType, String branch);

    // 银行特殊业务
    List<DeviceInfo> getDevicesByBranch(String branch);

    // 银行业务统计 - 保修预警
    List<Map<String, Object>> getWarrantyAlertDevices();

    // 银行业务统计 - 故障分析
    Map<String, Object> getFaultAnalysis();
}