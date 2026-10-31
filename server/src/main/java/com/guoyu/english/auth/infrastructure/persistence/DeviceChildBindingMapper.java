package com.guoyu.english.auth.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 设备被明确授权的儿童范围。
 */
@Mapper
public interface DeviceChildBindingMapper {

    @Insert("""
            INSERT INTO device_child_binding (device_id, child_id)
            VALUES (#{deviceId}, #{childId})
            """)
    int insert(@Param("deviceId") UUID deviceId, @Param("childId") long childId);

    @Delete("""
            DELETE FROM device_child_binding
            WHERE device_id = #{deviceId}
            """)
    int deleteByDevice(UUID deviceId);

    @Select("""
            SELECT child_id
            FROM device_child_binding
            WHERE device_id = #{deviceId}
            ORDER BY child_id
            """)
    List<Long> listChildIds(UUID deviceId);
}
