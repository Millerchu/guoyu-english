package com.guoyu.english.course.infrastructure;

import java.math.BigDecimal;

/**
 * 句子以及这一课版本钉住的句子音频。
 */
public class LessonSentenceRow {

    private String sentenceId;

    private String english;

    private String chinese;

    private String audioStorageKey;

    private String audioSource;

    private String audioMime;

    private Long audioSizeBytes;

    private String audioSha256;

    private String audioTool;

    private String audioVoice;

    private String audioStyle;

    private BigDecimal audioSpeechRate;

    private Integer audioPauseBetweenWordsMs;

    private Integer audioPauseBetweenSentencesMs;

    private Integer audioPauseAfterPromptMs;

    private String audioSourceText;

    private String audioGeneratedAt;

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

    public String getAudioStorageKey() {
        return audioStorageKey;
    }

    public void setAudioStorageKey(String audioStorageKey) {
        this.audioStorageKey = audioStorageKey;
    }

    public String getAudioSource() {
        return audioSource;
    }

    public void setAudioSource(String audioSource) {
        this.audioSource = audioSource;
    }

    public String getAudioMime() {
        return audioMime;
    }

    public void setAudioMime(String audioMime) {
        this.audioMime = audioMime;
    }

    public Long getAudioSizeBytes() {
        return audioSizeBytes;
    }

    public void setAudioSizeBytes(Long audioSizeBytes) {
        this.audioSizeBytes = audioSizeBytes;
    }

    public String getAudioSha256() {
        return audioSha256;
    }

    public void setAudioSha256(String audioSha256) {
        this.audioSha256 = audioSha256;
    }

    public String getAudioTool() {
        return audioTool;
    }

    public void setAudioTool(String audioTool) {
        this.audioTool = audioTool;
    }

    public String getAudioVoice() {
        return audioVoice;
    }

    public void setAudioVoice(String audioVoice) {
        this.audioVoice = audioVoice;
    }

    public String getAudioStyle() {
        return audioStyle;
    }

    public void setAudioStyle(String audioStyle) {
        this.audioStyle = audioStyle;
    }

    public BigDecimal getAudioSpeechRate() {
        return audioSpeechRate;
    }

    public void setAudioSpeechRate(BigDecimal audioSpeechRate) {
        this.audioSpeechRate = audioSpeechRate;
    }

    public Integer getAudioPauseBetweenWordsMs() {
        return audioPauseBetweenWordsMs;
    }

    public void setAudioPauseBetweenWordsMs(Integer audioPauseBetweenWordsMs) {
        this.audioPauseBetweenWordsMs = audioPauseBetweenWordsMs;
    }

    public Integer getAudioPauseBetweenSentencesMs() {
        return audioPauseBetweenSentencesMs;
    }

    public void setAudioPauseBetweenSentencesMs(Integer audioPauseBetweenSentencesMs) {
        this.audioPauseBetweenSentencesMs = audioPauseBetweenSentencesMs;
    }

    public Integer getAudioPauseAfterPromptMs() {
        return audioPauseAfterPromptMs;
    }

    public void setAudioPauseAfterPromptMs(Integer audioPauseAfterPromptMs) {
        this.audioPauseAfterPromptMs = audioPauseAfterPromptMs;
    }

    public String getAudioSourceText() {
        return audioSourceText;
    }

    public void setAudioSourceText(String audioSourceText) {
        this.audioSourceText = audioSourceText;
    }

    public String getAudioGeneratedAt() {
        return audioGeneratedAt;
    }

    public void setAudioGeneratedAt(String audioGeneratedAt) {
        this.audioGeneratedAt = audioGeneratedAt;
    }
}
