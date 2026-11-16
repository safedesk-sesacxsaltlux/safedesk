package com.safedesk.history.detail;

import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.entity.ConsultationStatus;
import com.safedesk.chat.entity.Message;
import com.safedesk.chat.repository.ConsultationRepository;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.history.detail.dto.ConsultationDetailResponseDTO;
import com.safedesk.history.detail.dto.ConsultationDetailResponseDTO.MessageInfo;
import com.safedesk.member.entity.Member;
import com.safedesk.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HIS-06 상담 상세 (docs/api/history.md). DB 없이 Repository 를 mock 으로 대신한다.
 */
class ConsultationDetailServiceTest {

    private static final long CONSULTATION_ID = 1L;
    private static final long CUSTOMER_ID = 8L;
    private static final long AGENT_ID = 4L;

    private ConsultationRepository consultationRepository;
    private MemberRepository memberRepository;
    private ConsultationDetailRepository detailRepository;
    private ConsultationDetailService service;

    @BeforeEach
    void setUp() {
        consultationRepository = mock(ConsultationRepository.class);
        memberRepository = mock(MemberRepository.class);
        detailRepository = mock(ConsultationDetailRepository.class);
        service = new ConsultationDetailService(consultationRepository, memberRepository, detailRepository);

        Member customer = mock(Member.class);
        when(customer.getLoginId()).thenReturn("user0007");
        Member agent = mock(Member.class);
        when(agent.getName()).thenReturn("김상담");
        when(memberRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(memberRepository.findById(AGENT_ID)).thenReturn(Optional.of(agent));

        // 메시지 4개: 고객 정상, 상담원, 고객 폭언(마스킹본 "*** 왜 이렇게 늦어요"), 시스템 경고
        when(detailRepository.findByConsultationIdOrderByCreatedAtAscMessageIdAsc(CONSULTATION_ID)).thenReturn(List.of(
                message(101L, "CUSTOMER", "수도요금 납부 방법 알려주세요", "NORMAL"),
                message(102L, "AGENT", "확인해 드릴게요", null),
                message(103L, "CUSTOMER", "*** 왜 이렇게 늦어요", "ABUSIVE"),
                message(104L, "SYSTEM", "[시스템 경고] 부적절한 표현이 감지되었습니다.", null)
        ));
        // 경고 2건: 수동 경고(연결 메시지 없음) 다음에 103번 메시지 자동 경고 → 103번은 2번째 경고
        when(detailRepository.findWarningMessageIdsInOrder(CONSULTATION_ID)).thenReturn(Arrays.asList(null, 103L));
        when(detailRepository.findAbuseOriginals(CONSULTATION_ID)).thenReturn(List.of(original(103L, "바보야 왜 이렇게 늦어요")));
        when(detailRepository.countViolations(CONSULTATION_ID)).thenReturn(1L);
    }

    @Test
    @DisplayName("없는 상담이면 404 CHAT_NOT_FOUND")
    void notFound() {
        when(consultationRepository.findById(CONSULTATION_ID)).thenReturn(Optional.empty());

        assertErrorCode(() -> service.getDetail(CONSULTATION_ID, 1L, "ADMIN"), ErrorCode.CHAT_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 고객의 상담이면 403 CHAT_ACCESS_DENIED")
    void otherCustomer() {
        givenConsultation(ConsultationStatus.TERMINATED, "요약");

        assertErrorCode(() -> service.getDetail(CONSULTATION_ID, 999L, "CUSTOMER"), ErrorCode.CHAT_ACCESS_DENIED);
    }

    @Test
    @DisplayName("다른 상담원이 맡은 상담이면 403 CHAT_ACCESS_DENIED")
    void otherAgent() {
        givenConsultation(ConsultationStatus.TERMINATED, "요약");

        assertErrorCode(() -> service.getDetail(CONSULTATION_ID, 999L, "AGENT"), ErrorCode.CHAT_ACCESS_DENIED);
    }

    @Test
    @DisplayName("역할 정보가 없으면 403")
    void noRole() {
        givenConsultation(ConsultationStatus.TERMINATED, "요약");

        assertErrorCode(() -> service.getDetail(CONSULTATION_ID, CUSTOMER_ID, null), ErrorCode.CHAT_ACCESS_DENIED);
    }

    @Test
    @DisplayName("진행 중인 상담이면 409 CHAT_NOT_CLOSED")
    void notClosed() {
        givenConsultation(ConsultationStatus.IN_PROGRESS, null);

        assertErrorCode(() -> service.getDetail(CONSULTATION_ID, 1L, "ADMIN"), ErrorCode.CHAT_NOT_CLOSED);
    }

    @Test
    @DisplayName("권한 확인이 진행 중 확인보다 먼저다 (남의 진행 중 상담 → 409 가 아니라 403)")
    void accessCheckedBeforeStatus() {
        givenConsultation(ConsultationStatus.IN_PROGRESS, null);

        assertErrorCode(() -> service.getDetail(CONSULTATION_ID, 999L, "CUSTOMER"), ErrorCode.CHAT_ACCESS_DENIED);
    }

    @Test
    @DisplayName("관리자: 전체 조회, 폭언은 마스킹본, 원문은 DB 에서 읽지도 않는다")
    void adminSeesMasked() {
        givenConsultation(ConsultationStatus.TERMINATED, "AI 응대 → 상담원 상담 → 반복 폭언으로 자동 종료");

        ConsultationDetailResponseDTO result = service.getDetail(CONSULTATION_ID, 1L, "ADMIN");

        assertThat(messageById(result, 103L).content()).isEqualTo("*** 왜 이렇게 늦어요");
        verify(detailRepository, never()).findAbuseOriginals(anyLong());
    }

    @Test
    @DisplayName("담당 상담원: 조회 가능, 폭언은 마스킹본")
    void assignedAgentSeesMasked() {
        givenConsultation(ConsultationStatus.TERMINATED, "요약");

        ConsultationDetailResponseDTO result = service.getDetail(CONSULTATION_ID, AGENT_ID, "AGENT");

        assertThat(messageById(result, 103L).content()).isEqualTo("*** 왜 이렇게 늦어요");
    }

    @Test
    @DisplayName("고객 본인: 본인 폭언 메시지는 원문, 나머지는 그대로")
    void customerSeesOwnOriginal() {
        givenConsultation(ConsultationStatus.TERMINATED, "요약");

        ConsultationDetailResponseDTO result = service.getDetail(CONSULTATION_ID, CUSTOMER_ID, "CUSTOMER");

        assertThat(messageById(result, 103L).content()).isEqualTo("바보야 왜 이렇게 늦어요");
        assertThat(messageById(result, 101L).content()).isEqualTo("수도요금 납부 방법 알려주세요");
    }

    @Test
    @DisplayName("기본 정보: 상담번호, 고객 아이디, 상담원 이름, 폭언 수, 위반 기록")
    void consultationInfo() {
        givenConsultation(ConsultationStatus.TERMINATED, "요약");

        ConsultationDetailResponseDTO.ConsultationInfo info =
                service.getDetail(CONSULTATION_ID, 1L, "ADMIN").consultation();

        assertThat(info.consultationNo()).isEqualTo("C-000001");
        assertThat(info.customerLoginId()).isEqualTo("user0007");
        assertThat(info.agentName()).isEqualTo("김상담");
        assertThat(info.abuseCount()).isEqualTo(1);
        assertThat(info.violationRecorded()).isTrue();
    }

    @Test
    @DisplayName("메시지: 보낸 사람 이름, 폭언 표시, 경고 번호(수동 경고도 순번에 포함)")
    void messageInfo() {
        givenConsultation(ConsultationStatus.TERMINATED, "요약");

        ConsultationDetailResponseDTO result = service.getDetail(CONSULTATION_ID, 1L, "ADMIN");

        assertThat(result.messages()).extracting(MessageInfo::senderName)
                .containsExactly("user0007", "김상담", "user0007", "시스템");
        assertThat(messageById(result, 101L).abuseStatus()).isNull();   // NORMAL 은 null 로 내려간다
        assertThat(messageById(result, 103L).abuseStatus()).isEqualTo("ABUSIVE");
        assertThat(messageById(result, 103L).warningNo()).isEqualTo(2);
        assertThat(messageById(result, 101L).warningNo()).isNull();
    }

    @Test
    @DisplayName("요약이 있으면 DONE")
    void summaryDone() {
        givenConsultation(ConsultationStatus.TERMINATED, "AI 응대 → 고객 종료");

        assertThat(service.getDetail(CONSULTATION_ID, 1L, "ADMIN").summary().summaryStatus()).isEqualTo("DONE");
    }

    @Test
    @DisplayName("요약 실패(\"요약 없음\")나 아직 없음이면 NONE, 메시지는 정상 조회")
    void summaryNone() {
        givenConsultation(ConsultationStatus.CLOSED, ConsultationDetailService.SUMMARY_FAILED_TEXT);
        ConsultationDetailResponseDTO failed = service.getDetail(CONSULTATION_ID, 1L, "ADMIN");

        givenConsultation(ConsultationStatus.CLOSED, null);
        ConsultationDetailResponseDTO missing = service.getDetail(CONSULTATION_ID, 1L, "ADMIN");

        assertThat(failed.summary().summaryStatus()).isEqualTo("NONE");
        assertThat(missing.summary().summaryStatus()).isEqualTo("NONE");
        assertThat(failed.messages()).hasSize(4);
    }

    // ---- 준비용 도우미 ----

    private void givenConsultation(String status, String processSummary) {
        Consultation c = mock(Consultation.class);
        when(c.getConsultationId()).thenReturn(CONSULTATION_ID);
        when(c.getCustomerId()).thenReturn(CUSTOMER_ID);
        when(c.getAgentId()).thenReturn(AGENT_ID);
        when(c.getStatus()).thenReturn(status);
        when(c.getProcessSummary()).thenReturn(processSummary);
        when(consultationRepository.findById(CONSULTATION_ID)).thenReturn(Optional.of(c));
    }

    private static Message message(long id, String senderType, String content, String abuseStatus) {
        Message m = new Message();
        m.setConsultationId(CONSULTATION_ID);
        m.setSenderType(senderType);
        m.setContent(content);
        m.setAbuseStatus(abuseStatus);
        m.setCreatedAt(LocalDateTime.of(2026, 11, 12, 14, 20).plusSeconds(id));
        try {
            // messageId 는 DB 가 만드는 값이라 setter 가 없어 리플렉션으로 넣는다
            Field field = Message.class.getDeclaredField("messageId");
            field.setAccessible(true);
            field.set(m, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return m;
    }

    private static AbuseOriginalRow original(long messageId, String content) {
        return new AbuseOriginalRow() {
            @Override
            public Long getMessageId() {
                return messageId;
            }

            @Override
            public String getOriginalContent() {
                return content;
            }
        };
    }

    private static MessageInfo messageById(ConsultationDetailResponseDTO result, long messageId) {
        return result.messages().stream()
                .filter(m -> m.messageId() == messageId)
                .findFirst()
                .orElseThrow();
    }

    private static void assertErrorCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
