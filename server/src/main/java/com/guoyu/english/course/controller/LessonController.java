package com.guoyu.english.course.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.guoyu.english.common.api.ApiResponse;
import com.guoyu.english.common.api.ApiResponses;
import com.guoyu.english.course.application.LessonQueryService;
import com.guoyu.english.course.dto.LessonDetail;

/**
 * 已发布课程查询。设备令牌属于后续身份阶段，这里不读取令牌，也不返回儿童数据。
 */
@RestController
@RequestMapping("/api/v1/lessons")
public class LessonController {

    private final LessonQueryService lessonQueryService;

    public LessonController(LessonQueryService lessonQueryService) {
        this.lessonQueryService = lessonQueryService;
    }

    @GetMapping(path = "/{lessonId}", produces = "application/json;charset=UTF-8")
    public ApiResponse<LessonDetail> lesson(
            @PathVariable("lessonId") String lessonId,
            @RequestParam("version") int version) {
        return ApiResponses.ok(lessonQueryService.getLesson(lessonId, version));
    }
}
