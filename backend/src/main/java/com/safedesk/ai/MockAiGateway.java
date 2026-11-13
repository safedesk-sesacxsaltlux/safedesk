package com.safedesk.ai;

import com.safedesk.ai.dto.AiReplyRequest;
import com.safedesk.ai.dto.AiResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
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
            case NORMAL -> {
                sleep(ThreadLocalRandom.current().nextLong(500, 1001));   // 진짜 AI처럼 0.5~1초 뒤 답변
                yield "[목업] " + request.content() + "에 대한 답변입니다.";
            }
            case DELAY -> {
                sleep(timeoutMs + 1000);    // 제한 시간보다 오래 걸리게 해서 TIMEOUT 유도
                yield "늦은 답변";              // { } 블록 안에서 값을 돌려줄 땐 return 대신 yield
            }
            case ERROR -> throw new RuntimeException("AI 호출 중 에러가 발생했습니다");
        };
    }

    // NORMAL·DELAY가 같이 쓰는 잠자기. 깨워지면 예외로 바꿔서 바깥 reply()의 catch(Exception)이 잡게 한다
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();   // "깨워졌다"는 표시를 지우지 않고 남겨 둠
            throw new RuntimeException(e);
        }
    }
}
