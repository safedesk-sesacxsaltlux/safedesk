package com.safedesk.ai;

import com.safedesk.ai.dto.AiReplyRequest;
import com.safedesk.ai.dto.AiResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/* 구현체: 가짜 AI */
@Component   // "Spring아, 이 클래스 객체를 하나 만들어서 관리해줘"
public class MockAiGateway implements AiGateway {
    private final long timeoutMs;
    private AiMockMode mode;   // CUS-05에서 실행 중에 바꿀 수 있어야 하니 final 아님

    public MockAiGateway(@Value("${ai.timeout-ms}") long timeoutMs,
                         @Value("${ai.mock.mode}") AiMockMode mode) {
        this.timeoutMs = timeoutMs;
        this.mode = mode;   // "NORMAL" 문자열이 enum으로 자동 변환
    }

    @Override
    public AiResult reply(AiReplyRequest request) {

        long start = System.currentTimeMillis();     // 시작 시각
        try {
            String answer = CompletableFuture
                .supplyAsync(() -> callMock(request))
                .get(timeoutMs, TimeUnit.MILLISECONDS);
            return AiResult.success(answer, System.currentTimeMillis() - start);
        } catch (TimeoutException e) {
            return AiResult.failure(AiFailureType.TIMEOUT, System.currentTimeMillis() - start);
        } catch (Exception e) {
            return AiResult.failure(AiFailureType.ERROR, System.currentTimeMillis() - start);
        }
    }

    private String callMock(AiReplyRequest request) {
        return switch (mode) {
            case NORMAL -> "[목업] " + request.content() + "에 대한 답변입니다.";   // 문자열만!
            case DELAY -> {
                try {
                    Thread.sleep(timeoutMs + 1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);   // 바깥 reply()의 catch(Exception)이 잡아줌
                }
                yield "늦은 답변";              // { } 블록 안에서 값을 돌려줄 땐 return 대신 yield
            }
            case ERROR -> throw new RuntimeException("AI 호출 중 에러가 발생했습니다");
        };
    }
}
