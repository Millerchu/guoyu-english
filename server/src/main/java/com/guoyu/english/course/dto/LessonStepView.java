package com.guoyu.english.course.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 课的一步。听一步给词条，说和读给句子，认一步给题目。无关列表不输出。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"stepId", "stepType", "required", "lexemeIds", "sentenceIds", "questions"})
public class LessonStepView {

    private String stepId;

    private String stepType;

    private boolean required;

    private List<String> lexemeIds;

    private List<String> sentenceIds;

    private List<LessonQuestionView> questions;

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public String getStepType() {
        return stepType;
    }

    public void setStepType(String stepType) {
        this.stepType = stepType;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public List<String> getLexemeIds() {
        return lexemeIds;
    }

    public void setLexemeIds(List<String> lexemeIds) {
        this.lexemeIds = lexemeIds;
    }

    public List<String> getSentenceIds() {
        return sentenceIds;
    }

    public void setSentenceIds(List<String> sentenceIds) {
        this.sentenceIds = sentenceIds;
    }

    public List<LessonQuestionView> getQuestions() {
        return questions;
    }

    public void setQuestions(List<LessonQuestionView> questions) {
        this.questions = questions;
    }
}
