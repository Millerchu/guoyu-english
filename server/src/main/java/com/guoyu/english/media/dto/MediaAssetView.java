package com.guoyu.english.media.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 课程响应里的媒体引用。只放相对 storageKey，不放 NAS 绝对路径。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "storageKey",
        "source",
        "mime",
        "sizeBytes",
        "sha256",
        "tool",
        "voice",
        "style",
        "speechRate",
        "pauseBetweenWordsMs",
        "pauseBetweenSentencesMs",
        "pauseAfterPromptMs",
        "sourceText",
        "generatedAt"
})
public class MediaAssetView {

    private String storageKey;

    private String source;

    private String mime;

    private Long sizeBytes;

    private String sha256;

    private String tool;

    private String voice;

    private String style;

    private BigDecimal speechRate;

    private Integer pauseBetweenWordsMs;

    private Integer pauseBetweenSentencesMs;

    private Integer pauseAfterPromptMs;

    private String sourceText;

    private String generatedAt;

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMime() {
        return mime;
    }

    public void setMime(String mime) {
        this.mime = mime;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public String getTool() {
        return tool;
    }

    public void setTool(String tool) {
        this.tool = tool;
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public BigDecimal getSpeechRate() {
        return speechRate;
    }

    public void setSpeechRate(BigDecimal speechRate) {
        this.speechRate = speechRate;
    }

    public Integer getPauseBetweenWordsMs() {
        return pauseBetweenWordsMs;
    }

    public void setPauseBetweenWordsMs(Integer pauseBetweenWordsMs) {
        this.pauseBetweenWordsMs = pauseBetweenWordsMs;
    }

    public Integer getPauseBetweenSentencesMs() {
        return pauseBetweenSentencesMs;
    }

    public void setPauseBetweenSentencesMs(Integer pauseBetweenSentencesMs) {
        this.pauseBetweenSentencesMs = pauseBetweenSentencesMs;
    }

    public Integer getPauseAfterPromptMs() {
        return pauseAfterPromptMs;
    }

    public void setPauseAfterPromptMs(Integer pauseAfterPromptMs) {
        this.pauseAfterPromptMs = pauseAfterPromptMs;
    }

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }
}
