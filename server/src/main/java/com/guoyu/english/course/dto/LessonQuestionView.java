package com.guoyu.english.course.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 认一认的一道题。正确项用词条 id 表示，选项顺序与种子一致。
 */
@JsonPropertyOrder({"questionId", "correctLexemeId", "options"})
public class LessonQuestionView {

    private String questionId;

    private String correctLexemeId;

    private List<LessonOptionView> options = new ArrayList<>();

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getCorrectLexemeId() {
        return correctLexemeId;
    }

    public void setCorrectLexemeId(String correctLexemeId) {
        this.correctLexemeId = correctLexemeId;
    }

    public List<LessonOptionView> getOptions() {
        return options;
    }

    public void setOptions(List<LessonOptionView> options) {
        this.options = options;
    }
}
