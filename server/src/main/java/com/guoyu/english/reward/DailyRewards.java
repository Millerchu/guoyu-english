package com.guoyu.english.reward;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guoyu.english.sync.SyncMapper;

/** 调用方已锁定家庭同步流，所有投影与原事件共用一个事务。 */
@Component
public class DailyRewards {
    private static final int ANIMALS_ACTIVITY_ITEMS = 5;
    private final SyncMapper mapper;
    private final ObjectMapper json;

    public DailyRewards(SyncMapper mapper, ObjectMapper json) {
        this.mapper = mapper;
        this.json = json;
    }

    public void apply(long child, LocalDate day, UUID id, String step, String target, String epoch, LocalDate since) {
        String prefix = child + ":" + day + ":";
        if ("step-speak".equals(step)) {
            String key = prefix + "SPEAK_STAR:" + target;
            if (mapper.star(child, key, day, id) > 0) {
                log(child, "star_ledger:" + key, json.createObjectNode().put("grantKey", key).put("delta", 1));
                mapper.rewardSummary(child, epoch, since);
            }
            if (!mapper.allSentencesDone(child, day)) {
                return;
            }
        } else if (target != null && mapper.completedTargets(child, day, step) < ANIMALS_ACTIVITY_ITEMS) {
            return;
        }
        if (mapper.progress(child, day, step) == 0) {
            return;
        }
        log(child, "daily_step_progress:" + prefix + step,
                json.createObjectNode().put("learningDay", day.toString()).put("stepId", step).put("completed", true));
        if (mapper.checkin(child, day) > 0) {
            mapper.sticker(child, day);
            log(child, "daily_checkin:" + child + ":" + day,
                    json.createObjectNode().put("learningDay", day.toString()).put("sticker", "CHECKIN_STICKER"));
        }
    }

    private void log(long child, String resource, ObjectNode value) {
        mapper.change(child, resource, value.toString());
    }
}
