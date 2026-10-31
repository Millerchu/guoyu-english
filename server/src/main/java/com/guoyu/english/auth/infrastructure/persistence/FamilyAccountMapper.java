package com.guoyu.english.auth.infrastructure.persistence;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

/**
 * 家庭行。引导期间用行锁把并发请求串起来。
 */
@Mapper
public interface FamilyAccountMapper {

    @Insert("""
            INSERT INTO family_account (auth_generation, bootstrap_consumed_at, created_at)
            VALUES (
                #{authGeneration},
                CAST(#{bootstrapConsumedAt} AS timestamptz),
                CAST(#{createdAt} AS timestamptz)
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(FamilyAccountRow row);

    @Select("""
            SELECT id, auth_generation, bootstrap_consumed_at, created_at
            FROM family_account
            LIMIT 1
            FOR UPDATE
            """)
    FamilyAccountRow lockSingleton();

    @Select("""
            SELECT id, auth_generation, bootstrap_consumed_at, created_at
            FROM family_account
            WHERE id = #{id}
            FOR UPDATE
            """)
    FamilyAccountRow lockById(long id);
}
