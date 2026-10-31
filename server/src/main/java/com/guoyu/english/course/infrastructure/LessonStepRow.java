package com.guoyu.english.course.infrastructure;

/**
 * 步骤行。definition_json 由测试和种子对照，查询服务按关联表组装响应。
 */
public class LessonStepRow {

    private String stepId;

    private String stepType;

    private int sortOrder;

    private boolean requiredForFirstTask;

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

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isRequiredForFirstTask() {
        return requiredForFirstTask;
    }

    public void setRequiredForFirstTask(boolean requiredForFirstTask) {
        this.requiredForFirstTask = requiredForFirstTask;
    }
}
