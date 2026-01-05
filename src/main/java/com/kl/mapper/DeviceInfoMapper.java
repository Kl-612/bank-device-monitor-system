package com.kl.mapper;

import com.kl.entity.DeviceInfo;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    //  更新设备信息
    @Update("<script>" +
            "UPDATE device_info " +
            "<set>" +
            "  <if test='deviceName != null'>device_name = #{deviceName},</if>" +
            "  <if test='deviceType != null'>device_type = #{deviceType},</if>" +
            "  <if test='vendor != null'>vendor = #{vendor},</if>" +
            "  <if test='model != null'>model = #{model},</if>" +
            "  <if test='ipAddress != null'>ip_address = #{ipAddress},</if>" +
            "  <if test='location != null'>location = #{location},</if>" +
            "  <if test='branch != null'>branch = #{branch},</if>" +
            "  <if test='status != null'>status = #{status},</if>" +
            "  <if test='installDate != null'>install_date = #{installDate},</if>" +
            "  <if test='warrantyPeriod != null'>warranty_period = #{warrantyPeriod},</if>" +
            "  update_time = NOW()" +
            "</set>" +
            "WHERE id = #{id}" +
            "</script>")
    int update(DeviceInfo device);

    //  更新设备状态
    @Update("UPDATE device_info SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") String status);

    //  删除设备
    @Delete("DELETE FROM device_info WHERE id = #{id}")
    int deleteById(Integer id);

    //  根据ID查询单个设备（用于更新前的数据获取）
    @Select("SELECT * FROM device_info WHERE id = #{id}")
    DeviceInfo selectById(Integer id);

    @Select("SELECT * FROM device_info WHERE device_id = #{deviceId}")
    DeviceInfo selectByDeviceId(String deviceId);

    @Select("SELECT COUNT(*) FROM device_info")
    int countAll();

    @Select("SELECT status, COUNT(*) as count FROM device_info GROUP BY status")
    List<Map<String, Object>> countByStatus();

    @Select("SELECT * FROM device_info WHERE device_type = #{deviceType}")
    List<DeviceInfo> selectByDeviceType(String deviceType);

    @Select("SELECT * FROM device_info WHERE branch LIKE CONCAT('%', #{branch}, '%')")
    List<DeviceInfo> selectByBranch(String branch);

    // 设备搜索（关键词模糊搜索）
    @Select("SELECT * FROM device_info WHERE " +
            "device_name LIKE #{keyword} OR " +
            "device_id LIKE #{keyword} OR " +
            "location LIKE #{keyword} OR " +
            "branch LIKE #{keyword} OR " +
            "vendor LIKE #{keyword}")
    List<DeviceInfo> searchByKeyword(String keyword);

    // 保修预警（未来30天到期）
    @Select("SELECT device_id as deviceId, device_name as deviceName, device_type as deviceType, " +
            "branch, install_date as installDate, warranty_period as warrantyPeriod, " +
            "DATE_ADD(install_date, INTERVAL warranty_period MONTH) as warrantyEndDate, " +
            "DATEDIFF(DATE_ADD(install_date, INTERVAL warranty_period MONTH), CURDATE()) as daysRemaining " +
            "FROM device_info " +
            "WHERE DATE_ADD(install_date, INTERVAL warranty_period MONTH) " +
            "BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY) " +
            "ORDER BY daysRemaining ASC")
    List<Map<String, Object>> getWarrantyAlertDevices();

    // 故障分析
    @Select("SELECT fault_code as faultCode, " +
            "COUNT(*) as faultCount, " +  // MySQL返回Long
            "CAST(COALESCE(AVG(downtime_duration), 0) AS DECIMAL(10,2)) as avgFixTimeMinutes " +  // 明确返回DECIMAL
            "FROM device_fault_record " +
            "GROUP BY fault_code")
    List<Map<String, Object>> getFaultAnalysisData();

    // MyBatis返回Map需要额外处理，写一个默认方法
    default Map<String, Object> getFaultAnalysis() {
        List<Map<String, Object>> faultData = getFaultAnalysisData();

        Map<String, Object> result = new HashMap<>();
        result.put("faultAnalysis", faultData);

        // 计算总体MTTR
        double totalTime = 0;
        long totalFaults = 0;
        for (Map<String, Object> fault : faultData) {
            Long count = (Long) fault.get("faultCount");
            totalFaults += count;

            BigDecimal avgTimeBigDecimal = (BigDecimal) fault.get("avgFixTimeMinutes");
            double avgTime = avgTimeBigDecimal.doubleValue();

            totalTime += avgTime * count;
        }

        result.put("totalFaults", totalFaults);
        result.put("overallMTTR", totalFaults > 0 ?
                String.format("%.1f分钟", totalTime / totalFaults) : "无故障记录");

        return result;
    }

}