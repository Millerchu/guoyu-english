package com.guoyu.english.course.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.guoyu.english.media.dto.MediaAssetView;

/**
 * 一课版本里的句子。跟我说和读一句都引用这里的 sentenceId。
 */
@JsonPropertyOrder({"sentenceId", "english", "chinese", "audio"})
public class LessonSentenceView {

    private String sentenceId;

    private String english;

    private String chinese;

    private MediaAssetView audio;

    public String getSentenceId() {
        return sentenceId;
    }

    public void setSentenceId(String sentenceId) {
        this.sentenceId = sentenceId;
    }

    public String getEnglish() {
        return english;
    }

    public void setEnglish(String english) {
        this.english = english;
    }

    public String getChinese() {
        return chinese;
    }

    public void setChinese(String chinese) {
        this.chinese = chinese;
    }

    public MediaAssetView getAudio() {
        return audio;
    }

    public void setAudio(MediaAssetView audio) {
        this.audio = audio;
    }
}
