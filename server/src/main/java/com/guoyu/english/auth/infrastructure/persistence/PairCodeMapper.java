package com.guoyu.english.auth.infrastructure.persistence;

import java.time.OffsetDateTime;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 配对码的签发、锁定和消费。
 */
@Mapper
public interface PairCodeMapper {

    @Insert("""
            INSERT INTO pair_code (family_id, code_hash, device_name, expires_at, created_at)
            VALUES (
                #{familyId},
                #{codeHash},
                #{deviceName},
                CAST(#{expiresAt} AS timestamptz),
                CAST(#{createdAt} AS timestamptz)
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(PairCodeRow row);

    @Select("""
            SELECT id, family_id, code_hash, device_name, expires_at, consumed_at, created_at
            FROM pair_code
            WHERE code_hash = #{codeHash}
            FOR UPDATE
            """)
    PairCodeRow findByHashForUpdate(String codeHash);

    @Update("""
            UPDATE pair_code
            SET consumed_at = CAST(#{consumedAt} AS timestamptz)
            WHERE id = #{id}
              AND consumed_at IS NULL
            """)
    int markConsumed(@Param("id") long id, @Param("consumedAt") OffsetDateTime consumedAt);
}
