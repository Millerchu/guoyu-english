package com.guoyu.english.course.infrastructure;

/**
 * 某一步引用的句子 id，按 sort_order 排好。
 */
public class LessonStepSentenceRow {

    private String stepId;

    private String sentenceId;

    private int sortOrder;

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public String getSentenceId() {
        return sentenceId;
    }

    public void setSentenceId(String sentenceId) {
        this.sentenceId = sentenceId;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
