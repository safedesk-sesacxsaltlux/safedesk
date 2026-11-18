package com.safedesk.chat.service;

import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API-002 같은 상담을 상담원 두 명이 동시에 수락하거나(A), 상담원 한 명이 상담 두 건을
 * 동시에 수락할 때(B) 한 건만 성공하는지 검증한다.
 *
 * mock 으로는 재현되지 않아서 실제 MySQL 로 두 스레드를 동시에 보낸다.
 * DB 가 필요하므로 DB_USERNAME 환경 변수가 있을 때만 실행한다 (평소 ./gradlew test 에서는 건너뜀).
 * 테스트용 고객·상담원 상담을 새로 만들고, 끝나면 만든 행을 모두 지운다. 시드 데이터는 건드리지 않는다.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_USERNAME", matches = ".+")
class ChatAcceptConcurrencyTest {

    private static final long POLICY_ID = 1L;   // 시드 기본 정책

    @Autowired
    private ChatService chatService;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    private final List<Long> createdConsultationIds = new ArrayList<>();
    private final List<Long> createdCustomerIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        for (Long consultationId : createdConsultationIds) {
            jdbc.update("DELETE FROM consultation WHERE consultation_id = ?", consultationId);
        }
        for (Long customerId : createdCustomerIds) {
            jdbc.update("DELETE FROM member WHERE member_id = ?", customerId);
        }
        createdConsultationIds.clear();
        createdCustomerIds.clear();
    }

    // 타이밍에 따라 겹치지 않을 수도 있어서 여러 번 돌린다
    @RepeatedTest(10)
    @DisplayName("케이스 A: 상담원 2명이 같은 QUEUED 상담을 동시에 수락하면 1명만 성공한다")
    void twoAgentsAcceptSameConsultation() throws Exception {
        long agentA = 2L;   // 시드 agent01
        long agentB = 3L;   // 시드 agent02
        long consultationId = queueConsultation(newCustomer());

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<Outcome>> futures;
        try {
            futures = List.of(
                pool.submit(acceptTask(start, consultationId, agentA)),
                pool.submit(acceptTask(start, consultationId, agentB)));
            start.countDown();   // 두 스레드를 동시에 출발시킨다
        } finally {
            pool.shutdown();
        }

        List<Outcome> outcomes = new ArrayList<>();
        for (Future<Outcome> future : futures) {
            outcomes.add(future.get(10, TimeUnit.SECONDS));
        }

        long succeeded = outcomes.stream().filter(Outcome::success).count();
        long rejected = outcomes.stream()
            .filter(o -> !o.success() && o.errorCode() == ErrorCode.CHAT_ALREADY_ACCEPTED)
            .count();

        assertThat(succeeded).isEqualTo(1);
        assertThat(rejected).isEqualTo(1);

        Long winner = outcomes.stream().filter(Outcome::success).findFirst().orElseThrow().agentId();
        assertThat(winner).isIn(agentA, agentB);
        assertThat(getStatus(consultationId)).isEqualTo("IN_PROGRESS");
        assertThat(getAgentId(consultationId)).isEqualTo(winner);
    }

    @RepeatedTest(10)
    @DisplayName("케이스 B: 상담원 1명이 서로 다른 QUEUED 상담 2건을 동시에 수락하면 1건만 성공한다")
    void oneAgentAcceptsTwoConsultations() throws Exception {
        long agentId = 4L;   // 시드 agent03 (케이스 A 의 agent01·02 와 겹치지 않게 분리)
        long consultationX = queueConsultation(newCustomer());
        long consultationY = queueConsultation(newCustomer());

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<Outcome>> futures;
        try {
            futures = List.of(
                pool.submit(acceptTask(start, consultationX, agentId)),
                pool.submit(acceptTask(start, consultationY, agentId)));
            start.countDown();
        } finally {
            pool.shutdown();
        }

        List<Outcome> outcomes = new ArrayList<>();
        for (Future<Outcome> future : futures) {
            outcomes.add(future.get(10, TimeUnit.SECONDS));
        }

        long succeeded = outcomes.stream().filter(Outcome::success).count();
        long rejected = outcomes.stream()
            .filter(o -> !o.success() && o.errorCode() == ErrorCode.AGENT_BUSY)
            .count();

        assertThat(succeeded).isEqualTo(1);
        assertThat(rejected).isEqualTo(1);
        assertThat(count(
            "SELECT COUNT(*) FROM consultation WHERE agent_id = ? AND status = 'IN_PROGRESS'",
            agentId)).isEqualTo(1);
    }

    private Callable<Outcome> acceptTask(CountDownLatch start, long consultationId, long agentId) {
        return () -> {
            start.await();
            try {
                var response = chatService.acceptConsultation(consultationId, agentId);
                return new Outcome(true, null, response.getAgentId());
            } catch (BusinessException e) {
                return new Outcome(false, e.getErrorCode(), null);
            }
        };
    }

    private record Outcome(boolean success, ErrorCode errorCode, Long agentId) {
    }

    private long newCustomer() {
        long customerId = insert("""
            INSERT INTO member (login_id, password, name, role, status)
            VALUES (?, 'x', '동시수락테스트', 'CUSTOMER', 'ACTIVE')
            """, "it-acc-" + UUID.randomUUID().toString().substring(0, 8));
        createdCustomerIds.add(customerId);
        return customerId;
    }

    private long queueConsultation(long customerId) {
        long consultationId = insert("""
            INSERT INTO consultation (customer_id, policy_id, status, queued_at)
            VALUES (?, ?, 'QUEUED', NOW())
            """, customerId, POLICY_ID);
        createdConsultationIds.add(consultationId);
        return consultationId;
    }

    private String getStatus(long consultationId) {
        return jdbc.queryForObject("SELECT status FROM consultation WHERE consultation_id = ?",
            String.class, consultationId);
    }

    private Long getAgentId(long consultationId) {
        return jdbc.queryForObject("SELECT agent_id FROM consultation WHERE consultation_id = ?",
            Long.class, consultationId);
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
