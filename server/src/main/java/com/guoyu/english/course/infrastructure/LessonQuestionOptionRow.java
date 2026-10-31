package com.guoyu.english.course.infrastructure;

/**
 * 认一认题目和其中一个选项。同一题的多行由查询按选项顺序返回。
 */
public class LessonQuestionOptionRow {

    private String stepId;

    private String questionId;

    private String correctLexemeId;

    private int questionSortOrder;

    private String optionId;

    private String optionLexemeId;

    private int optionSortOrder;

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

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

    public int getQuestionSortOrder() {
        return questionSortOrder;
    }

    public void setQuestionSortOrder(int questionSortOrder) {
        this.questionSortOrder = questionSortOrder;
    }

    public String getOptionId() {
        return optionId;
    }

    public void setOptionId(String optionId) {
        this.optionId = optionId;
    }

    public String getOptionLexemeId() {
        return optionLexemeId;
    }

    public void setOptionLexemeId(String optionLexemeId) {
        this.optionLexemeId = optionLexemeId;
    }

    public int getOptionSortOrder() {
        return optionSortOrder;
    }

    public void setOptionSortOrder(int optionSortOrder) {
        this.optionSortOrder = optionSortOrder;
    }
}
