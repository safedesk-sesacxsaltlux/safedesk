package com.safedesk.chat.service;

import com.safedesk.chat.dto.ChatMessageRequestDTO;
import com.safedesk.moderation.dto.ConsultationTerminatedMessage;
import com.safedesk.moderation.dto.WarningIssuedMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * #59 같은 상담에 폭언 두 건이 동시에 들어와도 경고 수를 정확히 센다.
 *
 * mock 으로는 재현되지 않아서 실제 MySQL 로 두 스레드를 동시에 보낸다.
 * DB 가 필요하므로 DB_USERNAME 환경 변수가 있을 때만 실행한다 (평소 ./gradlew test 에서는 건너뜀).
 * 테스트용 고객·상담을 새로 만들고, 끝나면 만든 행을 모두 지운다. 시드 데이터는 건드리지 않는다.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_USERNAME", matches = ".+")
class ChatServiceConcurrencyTest {

    private static final long AGENT_ID = 2L;    // 시드 agent01
    private static final long POLICY_ID = 1L;   // 시드 기본 정책 (종료 기준 2회)

    @Autowired
    private ChatService chatService;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    private long customerId;
    private long consultationId;

    @BeforeEach
    void setUp() {
        customerId = insert("""
            INSERT INTO member (login_id, password, name, role, status)
            VALUES (?, 'x', '동시성테스트', 'CUSTOMER', 'ACTIVE')
            """, "it59-" + UUID.randomUUID().toString().substring(0, 8));
        consultationId = insert("""
            INSERT INTO consultation (customer_id, agent_id, policy_id, status, queued_at, accepted_at)
            VALUES (?, ?, ?, 'IN_PROGRESS', NOW(), NOW())
            """, customerId, AGENT_ID, POLICY_ID);
    }

    @AfterEach
    void tearDown() {
        // 외래키 순서대로 지운다
        jdbc.update("DELETE FROM warning WHERE consultation_id = ?", consultationId);
        jdbc.update("DELETE FROM sanction WHERE customer_id = ?", customerId);
        jdbc.update("DELETE FROM violation WHERE customer_id = ?", customerId);
        jdbc.update("DELETE FROM abuse_log WHERE consultation_id = ?", consultationId);
        jdbc.update("DELETE FROM message WHERE consultation_id = ?", consultationId);
        jdbc.update("DELETE FROM consultation WHERE consultation_id = ?", consultationId);
        jdbc.update("DELETE FROM member WHERE member_id = ?", customerId);
    }

    // 타이밍에 따라 겹치지 않을 수도 있어서 여러 번 돌린다
    @RepeatedTest(5)
    @DisplayName("폭언 두 건이 동시에 오면 두 번째에서 자동 종료되고, 경고 알림·종료 알림은 한 번씩만 나간다")
    void concurrentAbuseTerminatesOnSecond() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> results = List.of(
                pool.submit(() -> send(start, "시발 뭐야")),
                pool.submit(() -> send(start, "씨발 진짜")));
            start.countDown();   // 두 스레드를 동시에 출발시킨다
            for (Future<?> result : results) {
                result.get(10, TimeUnit.SECONDS);   // 교착 상태 등으로 실패하면 여기서 예외
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(count("SELECT COUNT(*) FROM warning WHERE consultation_id = ?", consultationId)).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM violation WHERE consultation_id = ?", consultationId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM consultation WHERE consultation_id = ?",
            String.class, consultationId)).isEqualTo("TERMINATED");

        ArgumentCaptor<Object> payloads = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(anyString(), payloads.capture());
        assertThat(payloads.getAllValues()).filteredOn(WarningIssuedMessage.class::isInstance).hasSize(1);
        assertThat(payloads.getAllValues()).filteredOn(ConsultationTerminatedMessage.class::isInstance).hasSize(1);
    }

    private void send(CountDownLatch start, String content) {
        ChatMessageRequestDTO request = new ChatMessageRequestDTO();
        request.setSenderType("CUSTOMER");
        request.setSenderId(customerId);
        request.setMessageContent(content);
        try {
            start.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        chatService.sendMessage(consultationId, request);
    }

    private long insert(String sql, Object... args) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    private long count(String sql, Object... args) {
        Long value = jdbc.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }
}
