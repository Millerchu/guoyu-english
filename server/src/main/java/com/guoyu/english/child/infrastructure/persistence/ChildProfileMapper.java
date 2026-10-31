package com.guoyu.english.child.infrastructure.persistence;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 儿童档案的插入和按家庭读取。
 */
@Mapper
public interface ChildProfileMapper {

    @Insert("""
            INSERT INTO child_profile (
                family_id, nickname, age_band, daily_minutes, timezone_id,
                timezone_version, profile_version, created_at
            )
            VALUES (
                #{familyId},
                #{nickname},
                #{ageBand},
                #{dailyMinutes},
                #{timezoneId},
                #{timezoneVersion},
                #{profileVersion},
                CAST(#{createdAt} AS timestamptz)
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(ChildProfileRow row);

    @Select("""
            SELECT id, family_id, nickname, age_band, daily_minutes, timezone_id,
                   timezone_version, profile_version, pending_timezone,
                   pending_timezone_effective_at, issued_through, created_at
            FROM child_profile
            WHERE id = #{id}
            """)
    ChildProfileRow findById(long id);

    @Select("""
            SELECT id, family_id, nickname, age_band, daily_minutes, timezone_id,
                   timezone_version, profile_version, pending_timezone,
                   pending_timezone_effective_at, issued_through, created_at
            FROM child_profile
            WHERE family_id = #{familyId}
            ORDER BY id
            """)
    List<ChildProfileRow> listByFamily(long familyId);

    @Select("""
            <script>
            SELECT id
            FROM child_profile
            WHERE family_id = #{familyId}
              AND id IN
              <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
              </foreach>
            ORDER BY id
            </script>
            """)
    List<Long> findExistingIds(@Param("familyId") long familyId, @Param("ids") List<Long> ids);
}
