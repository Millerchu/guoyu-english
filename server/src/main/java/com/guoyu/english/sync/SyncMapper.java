package com.guoyu.english.sync;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SyncMapper {
    @Select("SELECT stream_epoch::text FROM family_account WHERE id=#{familyId}")
    String epoch(long familyId);

    // 同一家庭先锁后分配日志序号，避免较小序号比已拉取的大序号更晚提交。
    @Select("SELECT stream_epoch::text FROM family_account WHERE id=#{familyId} FOR UPDATE")
    String lockStream(long familyId);

    @Select("""
            SELECT d.device_id::text FROM client_device d JOIN device_child_binding b USING(device_id)
            JOIN child_profile c ON c.id=b.child_id AND c.family_id=d.family_id
            WHERE d.device_id=#{device} AND d.family_id=#{family} AND NOT d.revoked AND b.child_id=#{child}
            FOR SHARE OF d,b,c
            """)
    String boundDevice(UUID device, long family, long child);

    @Update("UPDATE learning_event SET commit_cursor=#{cursor} WHERE event_id=#{id}")
    void settle(UUID id, long cursor);

    @Select("SELECT commit_cursor FROM learning_event WHERE event_id=#{id}")
    long settledCursor(UUID id);

    @Select("SELECT device_id::text FROM client_device WHERE device_id=#{device} AND NOT revoked FOR SHARE")
    String activeDevice(UUID device);

    @Select("SELECT request_hash FROM learning_event WHERE event_id=#{id}")
    String hash(UUID id);

    @Insert("""
            INSERT INTO learning_event(event_id,child_id,device_id,learning_day,occurred_at,step_id,target_id,request_hash)
            VALUES (#{id},#{child},#{device},#{day},#{time},#{step},#{target},#{hash}) ON CONFLICT DO NOTHING
            """)
    int insertEvent(UUID id, long child, UUID device, LocalDate day, OffsetDateTime time,
                    String step, String target, String hash);

    @Select("""
            SELECT EXISTS(SELECT 1 FROM lesson_step_sentence WHERE lesson_id='animals-001'
              AND lesson_version=1 AND step_id='step-speak' AND sentence_id=#{target})
            """)
    boolean speakTarget(String target);

    @Select("""
            SELECT CASE #{step}
              WHEN 'step-listen' THEN EXISTS (SELECT 1 FROM lesson_lexeme WHERE lesson_id='animals-001'
                AND lesson_version=1 AND lexeme_id=#{target})
              WHEN 'step-recognize' THEN EXISTS (SELECT 1 FROM lesson_question WHERE lesson_id='animals-001'
                AND lesson_version=1 AND question_id=#{target})
              ELSE false END
            """)
    boolean activityTarget(String step, String target);

    @Select("""
            SELECT count(DISTINCT target_id) FROM learning_event WHERE child_id=#{child} AND learning_day=#{day}
              AND step_id=#{step}
            """)
    int completedTargets(long child, LocalDate day, String step);

    @Select("""
            SELECT NOT EXISTS (SELECT 1 FROM lesson_step_sentence s WHERE s.lesson_id='animals-001'
              AND s.lesson_version=1 AND s.step_id='step-speak'
              AND NOT EXISTS (SELECT 1 FROM learning_event e WHERE e.child_id=#{child} AND e.learning_day=#{day}
                AND e.step_id=s.step_id AND e.target_id=s.sentence_id))
            """)
    boolean allSentencesDone(long child, LocalDate day);

    @Insert("INSERT INTO daily_step_progress VALUES (#{child},#{day},#{step}) ON CONFLICT DO NOTHING")
    int progress(long child, LocalDate day, String step);

    @Insert("""
            INSERT INTO star_ledger(child_id, grant_key, learning_day, event_id, delta)
            VALUES (#{child},#{key},#{day},#{id},1) ON CONFLICT DO NOTHING
            """)
    int star(long child, String key, LocalDate day, UUID id);

    @Insert("""
            INSERT INTO daily_checkin SELECT #{child},#{day} WHERE
              (SELECT count(*) FROM daily_step_progress WHERE child_id=#{child} AND learning_day=#{day}
               AND step_id IN ('step-listen','step-speak','step-recognize'))=3 ON CONFLICT DO NOTHING
            """)
    int checkin(long child, LocalDate day);

    @Insert("INSERT INTO badge_award VALUES (#{child},#{day},'CHECKIN_STICKER')")
    void sticker(long child, LocalDate day);

    @Insert("INSERT INTO sync_change_log(child_id,resource,value) VALUES(#{child},#{resource},CAST(#{json} AS jsonb))")
    void change(long child, String resource, String json);

    @Insert("""
            WITH position AS (SELECT nextval(pg_get_serial_sequence('sync_change_log','seq')) AS seq)
            INSERT INTO sync_change_log(seq,child_id,resource,value) OVERRIDING SYSTEM VALUE
            SELECT p.seq, #{child}, 'reward_summary:' || CAST(#{child} AS text),
              jsonb_build_object('streamEpoch', CAST(#{epoch} AS text), 'settlementCursor', p.seq,
                'confirmedStars', coalesce(sum(s.delta),0),
                'grants', coalesce(jsonb_agg(jsonb_build_object('grantKey',s.grant_key,'delta',s.delta)
                  ORDER BY s.grant_key) FILTER (WHERE s.learning_day >= #{since}), '[]'::jsonb))
            FROM position p LEFT JOIN star_ledger s ON s.child_id=#{child} GROUP BY p.seq
            """)
    void rewardSummary(long child, String epoch, LocalDate since);

    @Select("SELECT coalesce(max(seq),0) FROM sync_change_log WHERE child_id=#{child}")
    long lastSeq(long child);

    @Select("""
            SELECT json_build_object('seq',seq,'resource',resource,'operation','UPSERT','value',value)::text
            FROM sync_change_log WHERE child_id=#{child} AND seq>#{cursor} ORDER BY seq LIMIT #{limit}
            """)
    List<String> page(long child, long cursor, int limit);
}
