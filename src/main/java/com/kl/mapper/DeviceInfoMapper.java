package com.kl.mapper;

import com.kl.entity.DeviceInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface DeviceInfoMapper {

    @Select("SELECT * FROM device_info ORDER BY update_time DESC")
    List<DeviceInfo> selectAll();

    @Select("SELECT * FROM device_info WHERE status = #{status}")
    List<DeviceInfo> selectByStatus(String status);
}