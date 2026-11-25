package com.guoyu.english.sync;

import java.util.List;

public record PullResult(String streamEpoch, List<PullChange> changes, String nextCursor, boolean hasMore) {
}
