package com.guoyu.english.auth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.guoyu.english.auth.infrastructure.AuthProperties;

/**
 * 打开身份配置绑定。引导码、会话时长和 Argon2 参数都从这里进入。
 */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AuthConfiguration {
}
