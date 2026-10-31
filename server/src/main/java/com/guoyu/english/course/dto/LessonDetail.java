package com.guoyu.english.course.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * GET /api/v1/lessons/{lessonId}?version= 的 data。
 */
@JsonPropertyOrder({
        "lessonId",
        "version",
        "themeId",
        "themeNameZh",
        "themeNameEn",
        "lexemes",
        "sentences",
        "steps"
})
public class LessonDetail {

    private String lessonId;

    private int version;

    private String themeId;

    private String themeNameZh;

    private String themeNameEn;

    private List<LessonLexemeView> lexemes = new ArrayList<>();

    private List<LessonSentenceView> sentences = new ArrayList<>();

    private List<LessonStepView> steps = new ArrayList<>();

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

    public List<LessonLexemeView> getLexemes() {
        return lexemes;
    }

    public void setLexemes(List<LessonLexemeView> lexemes) {
        this.lexemes = lexemes;
    }

    public List<LessonSentenceView> getSentences() {
        return sentences;
    }

    public void setSentences(List<LessonSentenceView> sentences) {
        this.sentences = sentences;
    }

    public List<LessonStepView> getSteps() {
        return steps;
    }

    public void setSteps(List<LessonStepView> steps) {
        this.steps = steps;
    }
}
