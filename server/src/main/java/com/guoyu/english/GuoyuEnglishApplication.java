package com.guoyu.english;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 果语服务端启动入口。业务包在后续阶段按需加入，本阶段只提供公共响应和健康检查。
 * MyBatis-Plus 会扫描本包下的 {@code @Mapper} 接口。当前没有 Mapper。
 */
@SpringBootApplication
public class GuoyuEnglishApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuoyuEnglishApplication.class, args);
    }
}
