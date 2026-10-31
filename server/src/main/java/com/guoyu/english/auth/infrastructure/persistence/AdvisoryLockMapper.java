package com.guoyu.english.auth.infrastructure.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 家庭还不存在时，用事务级咨询锁挡住并发引导。
 */
@Mapper
public interface AdvisoryLockMapper {

    @Select("SELECT pg_advisory_xact_lock(#{lockKey})")
    String lock(long lockKey);
}
