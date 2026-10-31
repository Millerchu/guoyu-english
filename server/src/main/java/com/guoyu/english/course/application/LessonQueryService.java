package com.guoyu.english.course.application;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.guoyu.english.common.api.ApiCode;
import com.guoyu.english.common.api.ApiException;
import com.guoyu.english.common.web.RequestIds;
import com.guoyu.english.course.domain.StepType;
import com.guoyu.english.course.dto.LessonDetail;
import com.guoyu.english.course.dto.LessonLexemeView;
import com.guoyu.english.course.dto.LessonOptionView;
import com.guoyu.english.course.dto.LessonQuestionView;
import com.guoyu.english.course.dto.LessonSentenceView;
import com.guoyu.english.course.dto.LessonStepView;
import com.guoyu.english.course.infrastructure.LessonHeaderRow;
import com.guoyu.english.course.infrastructure.LessonLexemeRow;
import com.guoyu.english.course.infrastructure.LessonQueryMapper;
import com.guoyu.english.course.infrastructure.LessonQuestionOptionRow;
import com.guoyu.english.course.infrastructure.LessonSentenceRow;
import com.guoyu.english.course.infrastructure.LessonStepRow;
import com.guoyu.english.course.infrastructure.LessonStepSentenceRow;
import com.guoyu.english.media.domain.MediaSource;
import com.guoyu.english.media.domain.MediaStorageKeys;
import com.guoyu.english.media.dto.MediaAssetView;

/**
 * 按已发布的课版本组装查询结果。未知课或未知版本都是参数错误，不返回别的版本顶上。
 */
@Service
public class LessonQueryService {

    private static final Logger log = LoggerFactory.getLogger(LessonQueryService.class);

    private static final Pattern LESSON_ID = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    private static final int MAX_LESSON_ID_LENGTH = 64;

    private final LessonQueryMapper lessonQueryMapper;

    public LessonQueryService(LessonQueryMapper lessonQueryMapper) {
        this.lessonQueryMapper = lessonQueryMapper;
    }

    @Transactional(readOnly = true)
    public LessonDetail getLesson(String lessonId, int version) {
        if (!validLessonId(lessonId) || version < 1) {
            throw invalid();
        }
        log.info("查询课程, requestId={}, lessonId={}, version={}", RequestIds.current(), lessonId, version);
        LessonHeaderRow header = lessonQueryMapper.findLesson(lessonId, version);
        if (header == null) {
            throw invalid();
        }
        List<LessonLexemeRow> lexemeRows = lessonQueryMapper.findLexemes(lessonId, version);
        List<LessonSentenceRow> sentenceRows = lessonQueryMapper.findSentences(lessonId, version);
        List<String> lexemeIds = new ArrayList<>();
        List<LessonLexemeView> lexemes = new ArrayList<>();
        for (LessonLexemeRow row : lexemeRows) {
            lexemeIds.add(row.getLexemeId());
            lexemes.add(toLexeme(row));
        }
        Map<String, LessonSentenceView> sentencesById = new LinkedHashMap<>();
        for (LessonSentenceRow row : sentenceRows) {
            sentencesById.put(row.getSentenceId(), toSentence(row));
        }
        List<LessonStepSentenceRow> stepSentences = lessonQueryMapper.findStepSentences(lessonId, version);
        List<LessonQuestionOptionRow> questionRows = lessonQueryMapper.findQuestionOptions(lessonId, version);
        List<LessonStepView> steps = new ArrayList<>();
        for (LessonStepRow row : lessonQueryMapper.findSteps(lessonId, version)) {
            steps.add(toStep(row, lexemeIds, sentencesById, stepSentences, questionRows));
        }
        LessonDetail detail = new LessonDetail();
        detail.setLessonId(header.getLessonId());
        detail.setVersion(header.getVersion());
        detail.setThemeId(header.getThemeId());
        detail.setThemeNameZh(header.getThemeNameZh());
        detail.setThemeNameEn(header.getThemeNameEn());
        detail.setLexemes(lexemes);
        detail.setSentences(new ArrayList<>(sentencesById.values()));
        detail.setSteps(steps);
        return detail;
    }

    private LessonStepView toStep(LessonStepRow row, List<String> lexemeIds,
            Map<String, LessonSentenceView> sentencesById, List<LessonStepSentenceRow> stepSentences,
            List<LessonQuestionOptionRow> questionRows) {
        LessonStepView step = new LessonStepView();
        step.setStepId(row.getStepId());
        StepType stepType = StepType.fromDatabase(row.getStepType());
        step.setStepType(stepType.name());
        step.setRequired(row.isRequiredForFirstTask());
        if (stepType == StepType.LISTEN) {
            step.setLexemeIds(List.copyOf(lexemeIds));
        } else if (stepType == StepType.SPEAK || stepType == StepType.READ) {
            step.setSentenceIds(sentenceIds(row.getStepId(), sentencesById, stepSentences));
        } else if (stepType == StepType.RECOGNIZE) {
            step.setQuestions(questions(row.getStepId(), questionRows));
        }
        return step;
    }

    private List<String> sentenceIds(String stepId, Map<String, LessonSentenceView> sentencesById,
            List<LessonStepSentenceRow> stepSentences) {
        List<String> ids = new ArrayList<>();
        for (LessonStepSentenceRow link : stepSentences) {
            if (!stepId.equals(link.getStepId())) {
                continue;
            }
            if (!sentencesById.containsKey(link.getSentenceId())) {
                throw broken();
            }
            ids.add(link.getSentenceId());
        }
        return ids;
    }

    private List<LessonQuestionView> questions(String stepId, List<LessonQuestionOptionRow> rows) {
        Map<String, LessonQuestionView> questions = new LinkedHashMap<>();
        for (LessonQuestionOptionRow row : rows) {
            if (!stepId.equals(row.getStepId())) {
                continue;
            }
            LessonQuestionView question = questions.get(row.getQuestionId());
            if (question == null) {
                question = new LessonQuestionView();
                question.setQuestionId(row.getQuestionId());
                question.setCorrectLexemeId(row.getCorrectLexemeId());
                question.setOptions(new ArrayList<>());
                questions.put(row.getQuestionId(), question);
            } else if (!question.getCorrectLexemeId().equals(row.getCorrectLexemeId())) {
                throw broken();
            }
            LessonOptionView option = new LessonOptionView();
            option.setOptionId(row.getOptionId());
            option.setLexemeId(row.getOptionLexemeId());
            question.getOptions().add(option);
        }
        return new ArrayList<>(questions.values());
    }

    private LessonLexemeView toLexeme(LessonLexemeRow row) {
        LessonLexemeView view = new LessonLexemeView();
        view.setLexemeId(row.getLexemeId());
        view.setEnglish(row.getEnglish());
        view.setChinese(row.getChinese());
        MediaAssetView image = baseAsset(
                row.getImageStorageKey(),
                row.getImageSource(),
                row.getImageMime(),
                row.getImageSizeBytes(),
                row.getImageSha256());
        image.setTool(row.getImageTool());
        image.setGeneratedAt(row.getImageGeneratedAt());
        view.setImage(image);
        view.setAudio(audioAsset(
                row.getAudioStorageKey(),
                row.getAudioSource(),
                row.getAudioMime(),
                row.getAudioSizeBytes(),
                row.getAudioSha256(),
                row.getAudioTool(),
                row.getAudioVoice(),
                row.getAudioStyle(),
                row.getAudioSpeechRate(),
                row.getAudioPauseBetweenWordsMs(),
                row.getAudioPauseBetweenSentencesMs(),
                row.getAudioPauseAfterPromptMs(),
                row.getAudioSourceText(),
                row.getAudioGeneratedAt()));
        return view;
    }

    private LessonSentenceView toSentence(LessonSentenceRow row) {
        LessonSentenceView view = new LessonSentenceView();
        view.setSentenceId(row.getSentenceId());
        view.setEnglish(row.getEnglish());
        view.setChinese(row.getChinese());
        view.setAudio(audioAsset(
                row.getAudioStorageKey(),
                row.getAudioSource(),
                row.getAudioMime(),
                row.getAudioSizeBytes(),
                row.getAudioSha256(),
                row.getAudioTool(),
                row.getAudioVoice(),
                row.getAudioStyle(),
                row.getAudioSpeechRate(),
                row.getAudioPauseBetweenWordsMs(),
                row.getAudioPauseBetweenSentencesMs(),
                row.getAudioPauseAfterPromptMs(),
                row.getAudioSourceText(),
                row.getAudioGeneratedAt()));
        return view;
    }

    private MediaAssetView audioAsset(String storageKey, String source, String mime, Long sizeBytes, String sha256,
            String tool, String voice, String style, java.math.BigDecimal speechRate, Integer pauseBetweenWordsMs,
            Integer pauseBetweenSentencesMs, Integer pauseAfterPromptMs, String sourceText, String generatedAt) {
        MediaAssetView view = baseAsset(storageKey, source, mime, sizeBytes, sha256);
        view.setTool(tool);
        view.setVoice(voice);
        view.setStyle(style);
        view.setSpeechRate(speechRate);
        view.setPauseBetweenWordsMs(pauseBetweenWordsMs);
        view.setPauseBetweenSentencesMs(pauseBetweenSentencesMs);
        view.setPauseAfterPromptMs(pauseAfterPromptMs);
        view.setSourceText(sourceText);
        view.setGeneratedAt(generatedAt);
        return view;
    }

    private MediaAssetView baseAsset(String storageKey, String source, String mime, Long sizeBytes, String sha256) {
        if (mime == null || mime.isBlank() || sizeBytes == null || sizeBytes <= 0) {
            throw broken();
        }
        MediaAssetView view = new MediaAssetView();
        view.setStorageKey(MediaStorageKeys.requireRelative(storageKey));
        view.setSource(MediaSource.fromDatabase(source).name());
        view.setMime(mime);
        view.setSizeBytes(sizeBytes);
        view.setSha256(MediaStorageKeys.requireSha256(sha256));
        return view;
    }

    private boolean validLessonId(String lessonId) {
        return lessonId != null
                && lessonId.length() <= MAX_LESSON_ID_LENGTH
                && LESSON_ID.matcher(lessonId).matches();
    }

    private ApiException invalid() {
        return new ApiException(ApiCode.INVALID_ARGUMENT, ApiCode.INVALID_ARGUMENT.name());
    }

    private ApiException broken() {
        return new ApiException(ApiCode.INTERNAL_ERROR, ApiCode.INTERNAL_ERROR.name());
    }
}
