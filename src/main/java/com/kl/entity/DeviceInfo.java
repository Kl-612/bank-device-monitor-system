package com.kl.entity;

import lombok.Data;
import java.util.Date;

@Data
public class DeviceInfo {
    private Integer id;
    private String deviceId;
    private String deviceName;
    private String deviceType;
    private String vendor;
    private String model;
    private String ipAddress;
    private String location;
    private String branch;
    private String status;
    private Date installDate;
    private Integer warrantyPeriod;
    private Date createTime;
    private Date updateTime;
}