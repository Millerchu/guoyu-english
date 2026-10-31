package com.guoyu.english.auth.infrastructure.persistence;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 每个家庭一行 PIN 哈希。
 */
@Mapper
public interface ParentCredentialMapper {

    @Insert("""
            INSERT INTO parent_credential (family_id, pin_hash, updated_at)
            VALUES (#{familyId}, #{pinHash}, CAST(#{updatedAt} AS timestamptz))
            """)
    int insert(ParentCredentialRow row);

    @Select("""
            SELECT family_id, pin_hash, updated_at
            FROM parent_credential
            WHERE family_id = #{familyId}
            """)
    ParentCredentialRow findByFamilyId(long familyId);
}
