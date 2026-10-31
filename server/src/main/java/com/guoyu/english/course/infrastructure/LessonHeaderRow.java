package com.guoyu.english.course.infrastructure;

/**
 * 课程头查询结果。不直接作为接口响应。
 */
public class LessonHeaderRow {

    private String lessonId;

    private int version;

    private String themeId;

    private String themeNameZh;

    private String themeNameEn;

    public String getLessonId() {
        return lessonId;
    }

    public void setLessonId(String lessonId) {
        this.lessonId = lessonId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getThemeId() {
        return themeId;
    }

    public void setThemeId(String themeId) {
        this.themeId = themeId;
    }

    public String getThemeNameZh() {
        return themeNameZh;
    }

    public void setThemeNameZh(String themeNameZh) {
        this.themeNameZh = themeNameZh;
    }

    public String getThemeNameEn() {
        return themeNameEn;
    }

    public void setThemeNameEn(String themeNameEn) {
        this.themeNameEn = themeNameEn;
    }
}
