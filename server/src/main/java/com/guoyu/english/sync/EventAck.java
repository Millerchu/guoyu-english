package com.guoyu.english.sync;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventAck(String eventId, String status, String code, String streamEpoch, String commitCursor,
                       List<GrantOutcome> grantOutcomes) {
}
