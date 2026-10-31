package com.guoyu.english.auth.infrastructure.persistence;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 一张配对码可以覆盖多个儿童。
 */
@Mapper
public interface PairCodeChildMapper {

    @Insert("""
            INSERT INTO pair_code_child (pair_code_id, child_id)
            VALUES (#{pairCodeId}, #{childId})
            """)
    int insert(@Param("pairCodeId") long pairCodeId, @Param("childId") long childId);

    @Select("""
            SELECT child_id
            FROM pair_code_child
            WHERE pair_code_id = #{pairCodeId}
            ORDER BY child_id
            """)
    List<Long> listChildIds(long pairCodeId);
}
