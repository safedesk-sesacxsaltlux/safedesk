package com.safedesk.ai;

import com.safedesk.ai.dto.AiReplyRequest;
import com.safedesk.ai.dto.AiResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MockAiGatewayTest {

    @Test
    @DisplayName("NORMAL 모드는 0.5~1초 뒤 성공을 돌려준다")
    void normalModeReturnsSuccessAfterHalfToOneSecond() {
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
    @DisplayName("NORMAL 모드에서 FAQ 답변이 함께 오면 그 답변을 돌려준다")
    void normalModeReturnsFaqAnswerWhenGiven() {
        MockAiGateway gateway = new MockAiGateway(2000, AiMockMode.NORMAL);

        AiResult result = gateway.reply(new AiReplyRequest(1L, "수도요금 어떻게 내요?", "청구서, 자동납부로 낼 수 있습니다."));

        assertThat(result.success()).isTrue();
        assertThat(result.content()).isEqualTo("청구서, 자동납부로 낼 수 있습니다.");
    }

    @Test
    @DisplayName("ERROR 모드는 ERROR를 돌려준다")
    void errorModeReturnsError() {
        MockAiGateway gateway = new MockAiGateway(100, AiMockMode.ERROR);

        AiResult result = gateway.reply(new AiReplyRequest(1L, "수도요금"));

        assertThat(result.success()).isFalse();
        assertThat(result.failureType()).isEqualTo(AiFailureType.ERROR);
    }

    @Test
    @DisplayName("DELAY 모드는 제한 시간에 끊기고 TIMEOUT을 돌려준다")
    void delayModeTimesOutAndReturnsTimeout() {
        // 테스트를 빨리 끝내려고 제한 시간을 0.1초로 (실제 설정은 10초)
        MockAiGateway gateway = new MockAiGateway(100, AiMockMode.DELAY);

        AiResult result = gateway.reply(new AiReplyRequest(1L, "수도요금"));

        assertThat(result.success()).isFalse();
        assertThat(result.failureType()).isEqualTo(AiFailureType.TIMEOUT);
        assertThat(result.latencyMs()).isLessThan(1000L);   // 1.1초를 다 기다리지 않고 끊었는지
    }
}
