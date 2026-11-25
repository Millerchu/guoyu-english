package com.guoyu.english.sync;

import java.util.List;

public record PushResult(String streamEpoch, List<EventAck> results) {
}
