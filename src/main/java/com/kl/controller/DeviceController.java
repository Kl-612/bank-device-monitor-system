package com.kl.controller;

import com.kl.entity.DeviceInfo;
import com.kl.mapper.DeviceInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    @GetMapping
    public List<DeviceInfo> getAllDevices() {
        return deviceInfoMapper.selectAll();
    }

    @GetMapping("/status/{status}")
    public List<DeviceInfo> getDevicesByStatus(@PathVariable String status) {
        return deviceInfoMapper.selectByStatus(status);
    }
}