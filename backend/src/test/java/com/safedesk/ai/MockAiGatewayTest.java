package com.safedesk.ai;

// ← import는 여기에 (Option + Enter로 자동 추가)

import com.safedesk.ai.dto.AiReplyRequest;
import com.safedesk.ai.dto.AiResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MockAiGatewayTest {

    @Test
    void normal모드는_성공을_돌려준다() {
        // 1. 준비: 게이트웨이 만들기
        MockAiGateway gateway = new MockAiGateway(100, AiMockMode.NORMAL);

        // 2. 실행: reply() 호출
        AiResult result = gateway.reply(new AiReplyRequest(1L, "수도요금"));

        // 3. 확인: assertThat(...)
        assertThat(result.success()).isTrue();
        assertThat(result.content()).contains("수도요금");
    }

    @Test
    void error모드는_ERROR를_돌려준다() {
        MockAiGateway gateway = new MockAiGateway(100, AiMockMode.ERROR);

        AiResult result = gateway.reply(new AiReplyRequest(1L, "수도요금"));

        assertThat(result.success()).isFalse();
        assertThat(result.failureType()).isEqualTo(AiFailureType.ERROR);
    }

    @Test
    void delay모드는_DELAY를_돌려준다() {
        MockAiGateway gateway = new MockAiGateway(12000, AiMockMode.DELAY);

        AiResult result = gateway.reply(new AiReplyRequest(1L, "수도요금"));

        assertThat(result.success()).isFalse();
        assertThat(result.failureType()).isEqualTo(AiFailureType.TIMEOUT);
    }
}
