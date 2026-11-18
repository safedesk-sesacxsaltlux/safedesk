
package com.safedesk.chat.service;

import com.safedesk.chat.dto.*;
import com.safedesk.chat.entity.CloseReason;
import com.safedesk.chat.entity.ClosedBy;
import com.safedesk.chat.entity.Consultation;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.member.entity.Member;
import com.safedesk.member.repository.MemberRepository;
import com.safedesk.moderation.dto.AbuseFilterResult;
import com.safedesk.moderation.dto.ConsultationTerminatedMessage;
import com.safedesk.moderation.dto.WarningIssuedMessage;
import com.safedesk.moderation.entity.AbuseLog;
import com.safedesk.moderation.service.ModerationService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import com.safedesk.chat.repository.MessageRepository;
import com.safedesk.chat.repository.ConsultationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.safedesk.common.event.ConsultationClosedEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.safedesk.chat.entity.Message;
import com.safedesk.chat.entity.ConsultationStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ChatService {

    private final MessageRepository messageRepository;

    private final SimpMessagingTemplate simpMessagingTemplate;

    private final ConsultationRepository consultationRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final ModerationService moderationService;

    private final MemberRepository memberRepository;

    // 메시지 길이 (contract.md WebSocket 「메시지 길이」, CUS-02와 같음)
    private static final int MAX_CONTENT_LENGTH = 500;

    public List<ChatQueueResponseDTO> getQueueList() {

        List<Consultation> consultations = consultationRepository.findByStatusOrderByQueuedAtAsc(ConsultationStatus.QUEUED);
        List<ChatQueueResponseDTO> queueList = new ArrayList<>();

        // 고객 이름은 한 번에 조회한다 (대기 건마다 회원을 따로 조회하지 않도록)
        List<Long> customerIds = consultations.stream().map(Consultation::getCustomerId).distinct().toList();
        Map<Long, String> customerNames = memberRepository.findAllById(customerIds).stream()
            .collect(Collectors.toMap(Member::getMemberId, Member::getName));

        for (Consultation consultation : consultations) {
            ChatQueueResponseDTO chatQueueResponseDTO = new ChatQueueResponseDTO();

            chatQueueResponseDTO.setConsultationId(consultation.getConsultationId());
            chatQueueResponseDTO.setCustomerId(String.valueOf(consultation.getCustomerId()));
            chatQueueResponseDTO.setCustomerName(customerNames.get(consultation.getCustomerId()));
            chatQueueResponseDTO.setConsultationStatus(consultation.getStatus());
            chatQueueResponseDTO.setQueuedAt(consultation.getQueuedAt());
            chatQueueResponseDTO.setInquiryCategory(consultation.getInquiryCategory());
            chatQueueResponseDTO.setInquirySummary(consultation.getInquirySummary());

            if (consultation.getQueuedAt() == null) {
                chatQueueResponseDTO.setWaitingTime(0);
            } else {
                chatQueueResponseDTO.setWaitingTime(
                    Duration.between(consultation.getQueuedAt(), LocalDateTime.now()).getSeconds());
            }

            queueList.add(chatQueueResponseDTO);

        }

        return queueList;
    }

    public ChatService(
        MessageRepository messageRepository, SimpMessagingTemplate simpMessagingTemplate,
        ConsultationRepository consultationRepository, ApplicationEventPublisher eventPublisher,
        ModerationService moderationService, MemberRepository memberRepository) {
        this.messageRepository = messageRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.consultationRepository = consultationRepository;
        this.eventPublisher = eventPublisher;
        this.moderationService = moderationService;
        this.memberRepository = memberRepository;
    }

    /**
     * API-005 실시간 채팅 메시지 전송.
     *
     * @param memberId STOMP CONNECT 토큰으로 정한 보낸 사람. 클라이언트가 보낸 senderType·senderId 는 쓰지 않는다 (NFR-004)
     */
    @Transactional
    public void sendMessage(long consultationId, Long memberId, String content) {

        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 같은 상담의 메시지를 한 건씩 처리하려고 상담 행을 잠근다 (#59: 동시 폭언의 경고 수를 정확히 세기 위해).
        // 트랜잭션의 첫 조회여야 한다. 일반 조회가 먼저 있으면 REPEATABLE READ 스냅샷이 고정돼 다른 쪽 경고가 안 보이고,
        // 메시지 INSERT(외래키 공유 락) 뒤에 잠그면 두 트랜잭션이 서로를 기다리는 교착 상태가 된다.
        // 상담원 종료(API-006)·폭언 종료(MOD-03)와도 같은 잠금이라, 상태 확인과 저장 사이에 종료가 끼어들지 못한다 (체크리스트 E7).
        Consultation consultation = consultationRepository.findWithLockByConsultationId(consultationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_FOUND));

        // 보낸 사람은 이 상담의 고객이거나 수락한 상담원이어야 한다
        String senderType;
        if (memberId.equals(consultation.getCustomerId())) {
            senderType = "CUSTOMER";
        } else if (memberId.equals(consultation.getAgentId())) {
            senderType = "AGENT";
        } else {
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        // 상담원과 상담 중일 때만 보낼 수 있다. AI 상담 중 메시지는 CUS-02(REST)로 보낸다
        String status = consultation.getStatus();
        if (ConsultationStatus.CLOSED.equals(status) || ConsultationStatus.TERMINATED.equals(status)) {
            throw new BusinessException(ErrorCode.CHAT_CLOSED);
        }
        if (!ConsultationStatus.IN_PROGRESS.equals(status)) {
            throw new BusinessException(ErrorCode.CHAT_NOT_ACTIVE);
        }

        if (content == null || content.isBlank() || content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        // 고객 메시지만 검사한다. 상담원 메시지는 검사하지 않는다 (정책 4장)
        boolean shouldModerate = "CUSTOMER".equals(senderType);

        Message chatMessage = new Message();

        chatMessage.setCreatedAt(LocalDateTime.now());

        String original = content;
        AbuseFilterResult result = null;

        if (shouldModerate) {
            result = moderationService.processMessage(original);
            chatMessage.setContent(result.maskedContent());
            chatMessage.setAbuseStatus(result.abuseStatus().name());
        } else {
            chatMessage.setContent(original);
        }
        chatMessage.setSenderType(senderType);
        chatMessage.setSenderId(memberId);
        chatMessage.setConsultationId(consultationId);

        Message saved = messageRepository.save(chatMessage);

        String topic = "/topic/chats/" + consultationId;

        sendAfterCommit(topic, ChatMessageResponseDTO.from(saved));

        if (shouldModerate && result.detected()) {
            AbuseLog abuseLog = moderationService.saveOriginal(saved.getMessageId(), consultationId, original);

            // 자동 경고 (MOD-02). 중복·종료된 상담이면 empty 라서 아무것도 하지 않는다
            moderationService.applyAutoWarning(consultationId, abuseLog.getAbuseLogId())
                .ifPresent(warning -> {
                    // 종료 기준에 도달한 폭언(기본 2회째)에는 경고 대신 종료 알림만 가야 한다
                    if (!warning.shouldTerminate()) {
                        sendAfterCommit(topic, WarningIssuedMessage.from(warning));
                    } else {
                        // 종료 기준 도달 → 자동 종료(MOD-03). 이미 종료된 상담이면 empty 라서 아무것도 보내지 않는다
                        // terminateForAbuse() 뒤에는 영속성 컨텍스트가 비워지므로 consultation·saved 엔티티를 다시 쓰지 않는다
                        moderationService.terminateForAbuse(consultationId)
                            .ifPresent(terminated ->
                                sendAfterCommit(topic, ConsultationTerminatedMessage.from(terminated)));
                    }
                });
        }
    }

    // 커밋된 뒤에만 발행한다. 저장 중 예외로 롤백되면 발행하지 않는다 (원문 노출 없이 전송 보류, moderation.md)
    private void sendAfterCommit(String destination, Object payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    simpMessagingTemplate.convertAndSend(destination, payload);
                }
            });
        } else {
            simpMessagingTemplate.convertAndSend(destination, payload);   // 트랜잭션 없는 단위 테스트용
        }
    }

    @Transactional
    public ChatAcceptResponseDTO acceptConsultation(long consultationId, Long agentId) {
        // 상담원 행을 트랜잭션의 첫 조회로 잠근다. 같은 상담원이 서로 다른 상담 두 건을
        // 동시에 수락하면 상담 행 잠금만으로는 겹치지 않아 existsByAgentIdAndStatus 검사를
        // 둘 다 통과할 수 있기 때문이다 (동시 수락 테스트). 상담 행 잠금보다 먼저 둬서 항상 같은 순서로
        // 잠그게 하면 두 트랜잭션이 서로를 기다리는 교착 상태도 생기지 않는다.
        Member agent = memberRepository.findWithLockByMemberId(agentId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Consultation consultation = consultationRepository
            .findWithLockByConsultationId(consultationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_FOUND));

        if (consultationRepository.existsByAgentIdAndStatus(agentId, ConsultationStatus.IN_PROGRESS)) {
            throw new BusinessException(ErrorCode.AGENT_BUSY);
        }

        Consultation lastTerminated = consultationRepository.findFirstByAgentIdAndStatusOrderByClosedAtDesc(agentId, ConsultationStatus.TERMINATED)
            .orElse(null);

        if (lastTerminated != null && lastTerminated.getClosedAt() != null) {
            long elapsedMinutes = Duration.between(lastTerminated.getClosedAt(), LocalDateTime.now()).toMinutes();

            if (elapsedMinutes < AgentRecoveryService.RECOVERY_MINUTES) {
                throw new BusinessException(ErrorCode.AGENT_RECOVERING);
            }
        }

        if (consultationRepository.accept(consultationId, agentId, LocalDateTime.now()) == 0) {
            throw new BusinessException(ErrorCode.CHAT_ALREADY_ACCEPTED);
        }

        consultation = consultationRepository.findById(consultationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_FOUND));

        // 고객 대기 화면 → 채팅 화면 전환 (contract.md WebSocket 「CONSULTATION_ACCEPTED」, 체크리스트 6)
        // 상담원 이름은 처음에 잠근 상담원 행에서 가져온다
        sendAfterCommit("/topic/chats/" + consultationId,
            ConsultationAcceptedMessage.of(consultationId, agent.getName(), consultation.getAcceptedAt()));

        ChatAcceptResponseDTO response = new ChatAcceptResponseDTO();
        response.setConsultationId(consultation.getConsultationId());
        response.setCustomerId(consultation.getCustomerId());
        response.setAgentId(consultation.getAgentId());
        response.setConsultationStatus(consultation.getStatus());
        response.setAcceptedAt(consultation.getAcceptedAt());

        return response;
    }

    /**
     * API-006 상담 종료. 수락한 상담원(closedBy AGENT) 또는 상담 중인 고객(closedBy CUSTOMER)이 종료한다
     * (contract.md 「종료 처리」). AI 상담·대기 중 고객 종료는 CUS-04 cancel 이 맡는다.
     */
    @Transactional
    public ChatCloseResponseDTO closeConsultation(long consultationId, Long memberId) {
        Consultation consultation = consultationRepository
            .findWithLockByConsultationId(consultationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_FOUND));

        String closedBy;
        if (memberId != null && memberId.equals(consultation.getAgentId())) {
            closedBy = ClosedBy.AGENT;
        } else if (memberId != null && memberId.equals(consultation.getCustomerId())) {
            closedBy = ClosedBy.CUSTOMER;
        } else {
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        if (!ConsultationStatus.IN_PROGRESS.equals(consultation.getStatus())
            && !ConsultationStatus.CLOSED.equals(consultation.getStatus())
            && !ConsultationStatus.TERMINATED.equals(consultation.getStatus())) {
            throw new BusinessException(ErrorCode.CHAT_NOT_ACTIVE);
        }

        if (consultation.close(closedBy, CloseReason.NORMAL)) {
            eventPublisher.publishEvent(
                new ConsultationClosedEvent(consultationId)
            );
            // 이번 요청으로 실제 종료됐을 때만 알린다. 이미 종료된 상담이면 다시 보내지 않는다
            sendAfterCommit("/topic/chats/" + consultationId, ConsultationClosedMessage.from(consultation));
        }

        ChatCloseResponseDTO response = new ChatCloseResponseDTO();

        response.setConsultationId(consultation.getConsultationId());
        response.setConsultationStatus(consultation.getStatus());
        response.setAgentId(consultation.getAgentId());
        response.setClosedAt(consultation.getClosedAt());
        response.setClosedBy(consultation.getClosedBy());
        response.setCloseReason(consultation.getCloseReason());

        return response;
    }

}
