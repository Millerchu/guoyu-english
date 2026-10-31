package com.guoyu.english.auth.infrastructure.persistence;

import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * PIN 失败计数。锁定时间写在数据库里，进程重启不会清掉。
 */
@Mapper
public interface PinAttemptMapper {

    @Select("""
            SELECT family_id, device_id, failed_count, locked_until, updated_at
            FROM pin_attempt
            WHERE family_id = #{familyId}
              AND device_id = #{deviceId}
            FOR UPDATE
            """)
    PinAttemptRow findForUpdate(@Param("familyId") long familyId, @Param("deviceId") UUID deviceId);

    @Update("""
            INSERT INTO pin_attempt (family_id, device_id, failed_count, locked_until, updated_at)
            VALUES (
                #{familyId},
                #{deviceId},
                #{failedCount},
                CAST(#{lockedUntil} AS timestamptz),
                CAST(#{updatedAt} AS timestamptz)
            )
            ON CONFLICT (family_id, device_id) DO UPDATE
            SET failed_count = EXCLUDED.failed_count,
                locked_until = EXCLUDED.locked_until,
                updated_at = EXCLUDED.updated_at
            """)
    int upsert(PinAttemptRow row);
}
