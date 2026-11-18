package com.safedesk.moderation.service;

import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.repository.ConsultationRepository;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.moderation.dto.WarningHistoryResponseDTO;
import com.safedesk.moderation.entity.Warning;
import com.safedesk.moderation.repository.WarningRepository;
import com.safedesk.sanction.entity.SanctionPolicy;
import com.safedesk.sanction.repository.SanctionPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 경고 조회 (MOD-05).
 *
 * ModerationService 와 나눈 이유: ModerationService 는 ChatService.sendMessage() 트랜잭션 안에서만 돌도록
 * @ Transactional 을 붙이지 않았다. 화면에서 바로 부르는 읽기 전용 조회는 여기서 자기 트랜잭션으로 처리한다.
 */
@Service
public class WarningQueryService {

    private final ConsultationRepository consultationRepository;
    private final WarningRepository warningRepository;
    private final SanctionPolicyRepository sanctionPolicyRepository;

    public WarningQueryService(
        ConsultationRepository consultationRepository,
        WarningRepository warningRepository,
        SanctionPolicyRepository sanctionPolicyRepository) {
        this.consultationRepository = consultationRepository;
        this.warningRepository = warningRepository;
        this.sanctionPolicyRepository = sanctionPolicyRepository;
    }

    /**
     * MOD-05 상담별 경고 내역 조회.
     *
     * 역할(AGENT·ADMIN 인지)은 컨트롤러에서 이미 확인했다. 여기서는 「담당 상담원인지」만 본다.
     *
     * @param memberId 로그인한 사용자 ID (JWT)
     * @param isAdmin  관리자면 담당 여부와 상관없이 조회
     */
    @Transactional(readOnly = true)
    public WarningHistoryResponseDTO getWarnings(long consultationId, Long memberId, boolean isAdmin) {

        // 조회만 하므로 잠그지 않는다 (findWithLockByConsultationId 아님)
        Consultation consultation = consultationRepository.findById(consultationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_NOT_FOUND));

        // 관리자가 아니면 담당 상담원만. 수락 전 상담은 agentId 가 null 이라 equals 가 false → 403
        if (!isAdmin && !memberId.equals(consultation.getAgentId())) {
            throw new BusinessException(ErrorCode.CHAT_ACCESS_DENIED);
        }

        List<Warning> warnings = warningRepository
            .findByConsultationIdOrderByCreatedAtAscWarningIdAsc(consultationId);

        // 「지금 활성 정책」이 아니라 「상담에 저장된 정책」 기준. MOD-02 자동 경고와 같아야 화면 숫자(1 / 2)가 맞다
        SanctionPolicy policy = sanctionPolicyRepository.findByIdOrLatest(consultation.getPolicyId());
        int closeThreshold = policy.getCloseThreshold();

        return WarningHistoryResponseDTO.of(consultationId, warnings, closeThreshold);
    }
}
