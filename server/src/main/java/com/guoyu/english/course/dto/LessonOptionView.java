package com.guoyu.english.course.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 认一认选项。只给选项 id 和词条 id，不在选项上放中文或英文。
 */
@JsonPropertyOrder({"optionId", "lexemeId"})
public class LessonOptionView {

    private String optionId;

    private String lexemeId;

    public String getOptionId() {
        return optionId;
    }

    public void setOptionId(String optionId) {
        this.optionId = optionId;
    }

    public String getLexemeId() {
        return lexemeId;
    }

    public void setLexemeId(String lexemeId) {
        this.lexemeId = lexemeId;
    }
}
