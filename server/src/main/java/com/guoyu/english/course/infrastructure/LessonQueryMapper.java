package com.guoyu.english.course.infrastructure;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 按课 id 和版本读取已发布内容。参数使用占位符，不拼接 SQL。
 */
@Mapper
public interface LessonQueryMapper {

    @Select("""
            SELECT l.lesson_id,
                   l.version,
                   t.theme_id,
                   t.name_zh AS theme_name_zh,
                   t.name_en AS theme_name_en
              FROM lesson l
              JOIN course_theme t ON t.theme_id = l.theme_id
             WHERE l.lesson_id = #{lessonId}
               AND l.version = #{version}
            """)
    LessonHeaderRow findLesson(@Param("lessonId") String lessonId, @Param("version") int version);

    @Select("""
            SELECT lx.lexeme_id,
                   lx.english,
                   lx.chinese,
                   img.storage_key AS image_storage_key,
                   img.source AS image_source,
                   img.mime AS image_mime,
                   img.size_bytes AS image_size_bytes,
                   rtrim(img.sha256) AS image_sha256,
                   img.tool AS image_tool,
                   to_char(img.generated_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"')
                       AS image_generated_at,
                   aud.storage_key AS audio_storage_key,
                   aud.source AS audio_source,
                   aud.mime AS audio_mime,
                   aud.size_bytes AS audio_size_bytes,
                   rtrim(aud.sha256) AS audio_sha256,
                   aud.tool AS audio_tool,
                   aud.voice AS audio_voice,
                   aud.style AS audio_style,
                   aud.speech_rate AS audio_speech_rate,
                   aud.pause_between_words_ms AS audio_pause_between_words_ms,
                   aud.pause_between_sentences_ms AS audio_pause_between_sentences_ms,
                   aud.pause_after_prompt_ms AS audio_pause_after_prompt_ms,
                   aud.source_text AS audio_source_text,
                   to_char(aud.generated_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"')
                       AS audio_generated_at
              FROM lesson_lexeme ll
              JOIN lexeme lx ON lx.lexeme_id = ll.lexeme_id
              JOIN media_asset img ON img.asset_id = ll.image_asset_id
              JOIN media_asset aud ON aud.asset_id = ll.audio_asset_id
             WHERE ll.lesson_id = #{lessonId}
               AND ll.lesson_version = #{version}
             ORDER BY ll.sort_order
            """)
    List<LessonLexemeRow> findLexemes(@Param("lessonId") String lessonId, @Param("version") int version);

    @Select("""
            SELECT s.sentence_id,
                   s.english,
                   s.chinese,
                   aud.storage_key AS audio_storage_key,
                   aud.source AS audio_source,
                   aud.mime AS audio_mime,
                   aud.size_bytes AS audio_size_bytes,
                   rtrim(aud.sha256) AS audio_sha256,
                   aud.tool AS audio_tool,
                   aud.voice AS audio_voice,
                   aud.style AS audio_style,
                   aud.speech_rate AS audio_speech_rate,
                   aud.pause_between_words_ms AS audio_pause_between_words_ms,
                   aud.pause_between_sentences_ms AS audio_pause_between_sentences_ms,
                   aud.pause_after_prompt_ms AS audio_pause_after_prompt_ms,
                   aud.source_text AS audio_source_text,
                   to_char(aud.generated_at AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"')
                       AS audio_generated_at
              FROM lesson_sentence s
              JOIN media_asset aud ON aud.asset_id = s.audio_asset_id
             WHERE s.lesson_id = #{lessonId}
               AND s.lesson_version = #{version}
             ORDER BY s.sort_order
            """)
    List<LessonSentenceRow> findSentences(@Param("lessonId") String lessonId, @Param("version") int version);

    @Select("""
            SELECT step_id,
                   step_type,
                   sort_order,
                   required_for_first_task
              FROM lesson_step
             WHERE lesson_id = #{lessonId}
               AND lesson_version = #{version}
             ORDER BY sort_order
            """)
    List<LessonStepRow> findSteps(@Param("lessonId") String lessonId, @Param("version") int version);

    @Select("""
            SELECT step_id,
                   sentence_id,
                   sort_order
              FROM lesson_step_sentence
             WHERE lesson_id = #{lessonId}
               AND lesson_version = #{version}
             ORDER BY step_id, sort_order
            """)
    List<LessonStepSentenceRow> findStepSentences(@Param("lessonId") String lessonId, @Param("version") int version);

    @Select("""
            SELECT q.step_id,
                   q.question_id,
                   q.correct_lexeme_id,
                   q.sort_order AS question_sort_order,
                   qo.option_id,
                   lo.lexeme_id AS option_lexeme_id,
                   qo.sort_order AS option_sort_order
              FROM lesson_question q
              JOIN lesson_question_option qo
                ON qo.lesson_id = q.lesson_id
               AND qo.lesson_version = q.lesson_version
               AND qo.question_id = q.question_id
              JOIN lesson_option lo
                ON lo.lesson_id = qo.lesson_id
               AND lo.lesson_version = qo.lesson_version
               AND lo.option_id = qo.option_id
             WHERE q.lesson_id = #{lessonId}
               AND q.lesson_version = #{version}
             ORDER BY q.sort_order, qo.sort_order
            """)
    List<LessonQuestionOptionRow> findQuestionOptions(@Param("lessonId") String lessonId, @Param("version") int version);
}
