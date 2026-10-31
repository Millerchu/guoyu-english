package com.guoyu.english.course.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.guoyu.english.media.dto.MediaAssetView;

/**
 * 一课版本里的词条，以及这一版钉住的图片和单词音频。
 */
@JsonPropertyOrder({"lexemeId", "english", "chinese", "image", "audio"})
public class LessonLexemeView {

    private String lexemeId;

    private String english;

    private String chinese;

    private MediaAssetView image;

    private MediaAssetView audio;

    public String getLexemeId() {
        return lexemeId;
    }

    public void setLexemeId(String lexemeId) {
        this.lexemeId = lexemeId;
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

    public MediaAssetView getImage() {
        return image;
    }

    public void setImage(MediaAssetView image) {
        this.image = image;
    }

    public MediaAssetView getAudio() {
        return audio;
    }

    public void setAudio(MediaAssetView audio) {
        this.audio = audio;
    }
}
