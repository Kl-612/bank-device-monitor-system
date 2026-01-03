package com.kl.controller;

import com.kl.entity.DeviceInfo;
import com.kl.mapper.DeviceInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    @Autowired
    private DeviceInfoMapper deviceInfoMapper;

    // GET方法
    @GetMapping
    public List<DeviceInfo> getAllDevices() {
        return deviceInfoMapper.selectAll();
    }

    // GET方法：根据状态查询设备
    @GetMapping("/status/{status}")
    public List<DeviceInfo> getDevicesByStatus(@PathVariable String status) {
        return deviceInfoMapper.selectByStatus(status);
    }

    //POST方法
    @PostMapping
    public ResponseEntity<Map<String, Object>> addDevice(@RequestBody DeviceInfo device) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 银行业务逻辑：设置默认值
            if (device.getStatus() == null) {
                device.setStatus("OFFLINE");  // 新设备默认离线
            }
            if (device.getCreateTime() == null) {
                device.setCreateTime(new Date());  // 当前时间
            }

            // 执行数据库插入
            int result = deviceInfoMapper.insert(device);

            if (result > 0) {
                response.put("success", true);
                response.put("message", "设备添加成功");
                response.put("data", device);  // 包含自动生成的id
                return ResponseEntity.status(201).body(response);
            } else {
                response.put("success", false);
                response.put("message", "设备添加失败");
                return ResponseEntity.status(500).body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "服务器错误: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}