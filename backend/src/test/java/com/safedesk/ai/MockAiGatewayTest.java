package com.safedesk.ai;

import com.safedesk.ai.dto.AiReplyRequest;
import com.safedesk.ai.dto.AiResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MockAiGatewayTest {

    @Test
    void normal모드는_0점5초에서_1초_뒤_성공을_돌려준다() {
        // 1. 준비: NORMAL은 최대 1초 걸리므로 제한 시간을 그보다 넉넉하게
        MockAiGateway gateway = new MockAiGateway(2000, AiMockMode.NORMAL);

        // 2. 실행
        AiResult result = gateway.reply(new AiReplyRequest(1L, "수도요금"));

        // 3. 확인
        assertThat(result.success()).isTrue();
        assertThat(result.content()).contains("수도요금");
        assertThat(result.latencyMs()).isBetween(500L, 1500L);
    }

    @Test
    void error모드는_ERROR를_돌려준다() {
        MockAiGateway gateway = new MockAiGateway(100, AiMockMode.ERROR);

        AiResult result = gateway.reply(new AiReplyRequest(1L, "수도요금"));

        assertThat(result.success()).isFalse();
        assertThat(result.failureType()).isEqualTo(AiFailureType.ERROR);
    }

    @Test
    void delay모드는_제한시간에_끊기고_TIMEOUT을_돌려준다() {
        // 테스트를 빨리 끝내려고 제한 시간을 0.1초로 (실제 설정은 10초)
        MockAiGateway gateway = new MockAiGateway(100, AiMockMode.DELAY);

        AiResult result = gateway.reply(new AiReplyRequest(1L, "수도요금"));

        assertThat(result.success()).isFalse();
        assertThat(result.failureType()).isEqualTo(AiFailureType.TIMEOUT);
        assertThat(result.latencyMs()).isLessThan(1000L);   // 1.1초를 다 기다리지 않고 끊었는지
    }
}
