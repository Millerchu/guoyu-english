package com.guoyu.english.auth.infrastructure.persistence;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 按范围和幂等键查找已经完成的请求。
 */
@Mapper
public interface RequestIdempotencyMapper {

    @Insert("""
            INSERT INTO request_idempotency (
                scope, idempotency_key, family_id, request_hash, resource_id, created_at
            )
            VALUES (
                #{scope},
                #{idempotencyKey},
                #{familyId},
                #{requestHash},
                #{resourceId},
                CAST(#{createdAt} AS timestamptz)
            )
            """)
    int insert(RequestIdempotencyRow row);

    @Select("""
            SELECT scope, idempotency_key, family_id, request_hash, resource_id, created_at
            FROM request_idempotency
            WHERE scope = #{scope}
              AND idempotency_key = #{idempotencyKey}
            """)
    RequestIdempotencyRow find(@Param("scope") String scope, @Param("idempotencyKey") String idempotencyKey);
}
