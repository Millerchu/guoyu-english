package com.guoyu.english.auth.domain;

/**
 * 幂等记录的业务范围。引导和建档分开，避免键碰巧相同时串档。
 */
public enum IdempotencyScope {

    BOOTSTRAP,
    CHILD_CREATE
}
