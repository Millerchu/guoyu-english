package com.guoyu.english.auth.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 家长会话的写入和按摘要查找。
 */
@Mapper
public interface ParentSessionMapper {

    @Insert("""
            INSERT INTO parent_session (family_id, device_id, token_hash, expires_at, created_at)
            VALUES (
                #{familyId},
                CAST(#{deviceId,jdbcType=OTHER} AS uuid),
                #{tokenHash},
                CAST(#{expiresAt} AS timestamptz),
                CAST(#{createdAt} AS timestamptz)
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(ParentSessionRow row);

    @Select("""
            SELECT id, family_id, device_id, token_hash, expires_at, revoked_at, created_at
            FROM parent_session
            WHERE token_hash = #{tokenHash}
            """)
    ParentSessionRow findByTokenHash(String tokenHash);

    @Update("""
            UPDATE parent_session
            SET revoked_at = CAST(#{revokedAt} AS timestamptz)
            WHERE device_id = #{deviceId}
              AND revoked_at IS NULL
            """)
    int revokeByDevice(@Param("deviceId") UUID deviceId, @Param("revokedAt") OffsetDateTime revokedAt);
}
