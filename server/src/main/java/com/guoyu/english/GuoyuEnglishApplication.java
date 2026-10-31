package com.guoyu.english;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 果语服务端启动入口。MyBatis-Plus 会扫描本包下的 {@code @Mapper} 接口。
 * 课程查询在 course 包，媒体来源在 media 包。身份与儿童档案在 auth、child 包。同步还不在本进程里。
 */
@SpringBootApplication
public class GuoyuEnglishApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuoyuEnglishApplication.class, args);
    }
}
