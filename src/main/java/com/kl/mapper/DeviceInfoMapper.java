package com.kl.mapper;

import com.kl.entity.DeviceInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DeviceInfoMapper {

    @Select("SELECT * FROM device_info ORDER BY update_time DESC")
    List<DeviceInfo> selectAll();

    @Select("SELECT * FROM device_info WHERE status = #{status}")
    List<DeviceInfo> selectByStatus(String status);

    @Insert("INSERT INTO device_info (device_id, device_name, device_type, vendor, model, ip_address, location, branch, status, install_date, warranty_period, create_time, update_time) " +
            "VALUES (#{deviceId}, #{deviceName}, #{deviceType}, #{vendor}, #{model}, #{ipAddress}, #{location}, #{branch}, #{status}, #{installDate}, #{warrantyPeriod}, #{createTime}, #{updateTime})")

    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DeviceInfo deviceInfo);
}