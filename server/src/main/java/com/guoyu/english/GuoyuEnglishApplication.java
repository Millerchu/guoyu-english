package com.guoyu.english;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 果语服务端启动入口。
 * MyBatis-Plus 会扫描本包下的 {@code @Mapper} 接口。S06 起包含身份与儿童档案。
 */
@SpringBootApplication
public class GuoyuEnglishApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuoyuEnglishApplication.class, args);
    }
}
