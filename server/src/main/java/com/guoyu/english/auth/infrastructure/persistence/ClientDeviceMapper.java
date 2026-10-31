package com.guoyu.english.auth.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 设备登记、按令牌摘要查找和撤销。
 */
@Mapper
public interface ClientDeviceMapper {

    @Insert("""
            INSERT INTO client_device (
                device_id, family_id, platform, display_name, token_hash, revoked, created_at
            )
            VALUES (
                #{deviceId},
                #{familyId},
                #{platform},
                #{displayName},
                #{tokenHash},
                #{revoked},
                CAST(#{createdAt} AS timestamptz)
            )
            """)
    int insert(ClientDeviceRow row);

    @Select("""
            SELECT device_id, family_id, platform, display_name, token_hash, revoked, revoked_at, created_at
            FROM client_device
            WHERE device_id = #{deviceId}
            FOR UPDATE
            """)
    ClientDeviceRow findByIdForUpdate(UUID deviceId);

    @Select("""
            SELECT device_id, family_id, platform, display_name, token_hash, revoked, revoked_at, created_at
            FROM client_device
            WHERE token_hash = #{tokenHash}
            """)
    ClientDeviceRow findByTokenHash(String tokenHash);

    @Select("""
            SELECT device_id, family_id, platform, display_name, token_hash, revoked, revoked_at, created_at
            FROM client_device
            WHERE family_id = #{familyId}
            ORDER BY created_at, device_id
            """)
    List<ClientDeviceRow> listByFamily(long familyId);

    @Update("""
            UPDATE client_device
            SET platform = #{platform},
                display_name = #{displayName},
                token_hash = #{tokenHash},
                revoked = FALSE,
                revoked_at = NULL
            WHERE device_id = #{deviceId}
            """)
    int rebind(ClientDeviceRow row);

    @Update("""
            UPDATE client_device
            SET revoked = TRUE,
                revoked_at = CAST(#{revokedAt} AS timestamptz)
            WHERE device_id = #{deviceId}
              AND revoked = FALSE
            """)
    int markRevoked(@Param("deviceId") UUID deviceId, @Param("revokedAt") OffsetDateTime revokedAt);
}
