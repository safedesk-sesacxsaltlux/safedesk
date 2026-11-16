package com.safedesk.history.detail;

import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.entity.ConsultationStatus;
import com.safedesk.chat.entity.Message;
import com.safedesk.chat.repository.ConsultationRepository;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.history.detail.dto.ConsultationDetailResponseDTO;
import com.safedesk.history.detail.dto.ConsultationDetailResponseDTO.ConsultationInfo;
import com.safedesk.history.detail.dto.ConsultationDetailResponseDTO.MessageInfo;
import com.safedesk.history.detail.dto.ConsultationDetailResponseDTO.SummaryInfo;
import com.safedesk.member.entity.Member;
import com.safedesk.member.repository.MemberRepository;
import com.safedesk.moderation.entity.AbuseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// HIS-06 상담 상세 (docs/api/history.md)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultationDetailService {

    private static final Set<String> CLOSED_STATUSES =
            Set.of(ConsultationStatus.CLOSED, ConsultationStatus.TERMINATED);
    // INT-01(PR #38) 이 요약 실패 시 저장하는 문구와 같아야 함
    static final String SUMMARY_FAILED_TEXT = "요약 없음";
    private static final String ABUSIVE = AbuseStatus.ABUSIVE.name();

    private final ConsultationRepository consultationRepository;
    private final MemberRepository memberRepository;
    private final ConsultationDetailRepository detailRepository;

    public ConsultationDetailResponseDTO getDetail(long consultationId, Long memberId, String role) {
        // 검사 순서 404 → 403 → 409
        // 권한보다 "진행 중"을 먼저 보면, 남의 상담이 진행 중이라는 정보가 새어 나간다
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_FOUND));

        checkAccess(consultation, memberId, role);

        if (!CLOSED_STATUSES.contains(consultation.getStatus())) {
            throw new BusinessException(ErrorCode.CHAT_NOT_CLOSED);
        }

        List<Message> messages = detailRepository.findByConsultationIdOrderByCreatedAtAscMessageIdAsc(consultationId);

        // 고객만 본인 폭언 원문을 본다. 상담원·관리자는 DB 조회 없이 빈 Map → 마스킹본 그대로
        Map<Long, String> originals = "CUSTOMER".equals(role)
                ? loadOriginals(consultationId)
                : Map.of();
        Map<Long, Integer> warningNos = numberWarnings(detailRepository.findWarningMessageIdsInOrder(consultationId));

        // 이름은 메시지마다가 아니라 한 번만 조회 (메시지 수만큼 쿼리 나가는 것 방지)
        String customerLoginId = memberRepository.findById(consultation.getCustomerId())
                .map(Member::getLoginId)
                .orElse(null);
        String agentName = consultation.getAgentId() == null ? null
                : memberRepository.findById(consultation.getAgentId()).map(Member::getName).orElse(null);

        List<MessageInfo> messageInfos = messages.stream()
                .map(m -> toMessageInfo(m, customerLoginId, agentName, originals, warningNos))
                .toList();
        long abuseCount = messages.stream()
                .filter(m -> ABUSIVE.equals(m.getAbuseStatus()))
                .count();

        return new ConsultationDetailResponseDTO(
                toConsultationInfo(consultation, customerLoginId, agentName, abuseCount,
                        detailRepository.countViolations(consultationId) > 0),
                toSummaryInfo(consultation),
                messageInfos
        );
    }

    // 고객=본인 상담, 상담원=맡은 상담, 관리자=전체
    private void checkAccess(Consultation consultation, Long memberId, String role) {
        boolean allowed = switch (role == null ? "" : role) {
            case "ADMIN" -> true;
            // getCustomerId() 는 long(기본형) → == 로 값 비교 OK
            case "CUSTOMER" -> memberId != null && memberId == consultation.getCustomerId();
            // getAgentId() 는 Long(객체) → 반드시 equals. 객체끼리 == 는 값이 같아도 false 가 날 수 있음
            case "AGENT" -> memberId != null && memberId.equals(consultation.getAgentId());
            default -> false;
        };
        if (!allowed) {
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED);
        }
    }

    private Map<Long, String> loadOriginals(long consultationId) {
        return detailRepository.findAbuseOriginals(consultationId).stream()
                .collect(Collectors.toMap(AbuseOriginalRow::getMessageId, AbuseOriginalRow::getOriginalContent));
    }

    // 경고 순서대로 1, 2, 3... 번호를 매기고 폭언 메시지와 연결된 것만 남긴다
    // 예) [8번 메시지, null(수동 경고), 11번 메시지] → {8:1, 11:3}
    private Map<Long, Integer> numberWarnings(List<Long> warningMessageIds) {
        Map<Long, Integer> result = new HashMap<>();
        for (int i = 0; i < warningMessageIds.size(); i++) {
            Long messageId = warningMessageIds.get(i);
            if (messageId != null) {
                result.put(messageId, i + 1);
            }
        }
        return result;
    }

    private MessageInfo toMessageInfo(Message m, String customerLoginId, String agentName,
                                      Map<Long, String> originals, Map<Long, Integer> warningNos) {
        boolean abusive = ABUSIVE.equals(m.getAbuseStatus());
        // 고객이 보낸 폭언만 원문 후보. originals 가 비어 있으면 마스킹본 그대로
        String content = abusive && "CUSTOMER".equals(m.getSenderType())
                ? originals.getOrDefault(m.getMessageId(), m.getContent())
                : m.getContent();

        return new MessageInfo(
                m.getMessageId(),
                m.getSenderType(),
                senderName(m.getSenderType(), customerLoginId, agentName),
                content,
                abusive ? ABUSIVE : null,
                warningNos.get(m.getMessageId()),   // 경고 없는 메시지는 null
                m.getCreatedAt()
        );
    }

    private String senderName(String senderType, String customerLoginId, String agentName) {
        return switch (senderType) {
            case "CUSTOMER" -> customerLoginId;
            case "AGENT" -> agentName;
            case "AI" -> "AI";
            case "SYSTEM" -> "시스템";
            default -> null;
        };
    }

    private ConsultationInfo toConsultationInfo(Consultation c, String customerLoginId, String agentName,
                                                long abuseCount, boolean violationRecorded) {
        return new ConsultationInfo(
                c.getConsultationId(),
                String.format("C-%06d", c.getConsultationId()),   // 123 → "C-000123"
                c.getStatus(),
                customerLoginId,
                agentName,
                c.getCreatedAt(),        // 응답 이름은 startedAt
                c.getQueuedAt(),
                c.getAcceptedAt(),
                c.getClosedAt(),
                c.getClosedBy(),
                c.getCloseReason(),
                abuseCount,
                violationRecorded
        );
    }

    // 요약 있으면 DONE, 실패("요약 없음")나 아직 없음(null)이면 NONE
    private SummaryInfo toSummaryInfo(Consultation c) {
        String processSummary = c.getProcessSummary();
        boolean done = processSummary != null && !SUMMARY_FAILED_TEXT.equals(processSummary);
        return new SummaryInfo(
                done ? "DONE" : "NONE",
                c.getInquiryCategory(),
                c.getInquirySummary(),
                processSummary,
                c.getResult()
        );
    }
}
