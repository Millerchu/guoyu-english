package com.guoyu.english.child.domain;

import org.junit.jupiter.api.Test;

import com.guoyu.english.child.api.CreateChildRequest;
import com.guoyu.english.common.api.ApiException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChildProfileRulesTest {

    @Test
    void defaultsMinutesAndShanghaiTimezone() {
        CreateChildRequest request = new CreateChildRequest();
        request.setNickname("  小球  ");
        request.setAgeBand("AGE_5");

        ChildDraft draft = ChildProfileRules.normalize(request);

        assertThat(draft.getNickname()).isEqualTo("小球");
        assertThat(draft.getAgeBand()).isEqualTo(AgeBand.AGE_5);
        assertThat(draft.getDailyMinutes()).isEqualTo(15);
        assertThat(draft.getTimezoneId()).isEqualTo("Asia/Shanghai");
    }

    @Test
    void rejectsUnknownTimezoneAndLongNickname() {
        CreateChildRequest offset = new CreateChildRequest();
        offset.setNickname("小球");
        offset.setAgeBand("AGE_6");
        offset.setTimezoneId("GMT+8");
        assertThatThrownBy(() -> ChildProfileRules.normalize(offset)).isInstanceOf(ApiException.class);

        CreateChildRequest nickname = new CreateChildRequest();
        nickname.setNickname("名".repeat(21));
        nickname.setAgeBand("AGE_7");
        assertThatThrownBy(() -> ChildProfileRules.normalize(nickname)).isInstanceOf(ApiException.class);
    }
}
