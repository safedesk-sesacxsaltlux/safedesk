package com.safedesk.sanction.service;

import com.safedesk.chat.entity.Consultation;
import com.safedesk.chat.repository.ConsultationRepository;
import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.common.response.PageResponse;
import com.safedesk.member.entity.Member;
import com.safedesk.member.repository.MemberRepository;
import com.safedesk.sanction.dto.PolicyAdminDTO;
import com.safedesk.sanction.dto.SanctionCustomerDetailDTO;
import com.safedesk.sanction.dto.SanctionDetailDTO;
import com.safedesk.sanction.dto.SanctionListItemDTO;
import com.safedesk.sanction.dto.SanctionReleaseDTO;
import com.safedesk.sanction.dto.SanctionReleaseResponseDTO;
import com.safedesk.sanction.dto.ViolationItemDTO;
import com.safedesk.sanction.dto.ViolationMessageDTO;
import com.safedesk.sanction.dto.ViolationOriginalDTO;
import com.safedesk.sanction.entity.Sanction;
import com.safedesk.sanction.entity.SanctionStatus;
import com.safedesk.sanction.entity.Violation;
import com.safedesk.sanction.repository.SanctionRepository;
import com.safedesk.sanction.repository.ViolationHistoryRow;
import com.safedesk.sanction.repository.ViolationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 관리자 제재 관리 (API-ADM-04~07). 제재 생성(MOD-04)은 SanctionService 가 맡는다
@Slf4j
@Service
public class SanctionAdminService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_DURATION_DAYS = 365;
    private static final int MAX_RELEASE_REASON_LENGTH = 255;

    private final SanctionRepository sanctionRepository;
    private final ViolationRepository violationRepository;
    private final MemberRepository memberRepository;
    private final ConsultationRepository consultationRepository;

    public SanctionAdminService(SanctionRepository sanctionRepository,
                                ViolationRepository violationRepository,
                                MemberRepository memberRepository,
                                ConsultationRepository consultationRepository) {
        this.sanctionRepository = sanctionRepository;
        this.violationRepository = violationRepository;
        this.memberRepository = memberRepository;
        this.consultationRepository = consultationRepository;
    }

    // API-ADM-04 제재 목록
    @Transactional(readOnly = true)
    public PageResponse<SanctionListItemDTO> getSanctions(String keyword, String status, Integer durationDays,
                                                          int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (durationDays != null && (durationDays < 1 || durationDays > MAX_DURATION_DAYS)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        String statusFilter = toStatusFilter(status);
        String keywordFilter = keyword == null || keyword.isBlank() ? null : keyword.trim();

        // 필터와 행마다 상태 계산이 같은 「지금」을 쓰도록 한 번만 구한다
        LocalDateTime now = LocalDateTime.now();
        Page<SanctionListItemDTO> result = sanctionRepository
            .findList(keywordFilter, statusFilter, durationDays, now, PageRequest.of(page, size))
            .map(row -> SanctionListItemDTO.of(row, now));
        return PageResponse.from(result);
    }

    // API-ADM-05 제재 상세
    @Transactional(readOnly = true)
    public SanctionDetailDTO getSanction(long sanctionId) {
        Sanction sanction = sanctionRepository.findById(sanctionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SANCTION_NOT_FOUND));
        Member customer = memberRepository.findById(sanction.getCustomerId())
            .orElseThrow(() -> new IllegalStateException("제재 고객이 없습니다: " + sanction.getCustomerId()));

        List<ViolationHistoryRow> rows = violationRepository.findHistoryByCustomerId(customer.getMemberId());
        List<ViolationItemDTO> violations = new ArrayList<>();
        long violationCount = 0;
        for (int i = 0; i < rows.size(); i++) {
            ViolationHistoryRow row = rows.get(i);
            violations.add(0, ViolationItemDTO.of(row, i + 1));
            if (row.getViolationId().equals(sanction.getViolationId())) {
                violationCount = i + 1;
            }
        }

        SanctionStatus status = sanction.statusAt(LocalDateTime.now());
        return new SanctionDetailDTO(
            sanction.getSanctionId(),
            SanctionCustomerDetailDTO.from(customer),
            violationCount,
            sanction.getSuspendDays(),
            sanction.getStartAt(),
            sanction.getEndAt(),
            status,
            status == SanctionStatus.SUSPENDED,
            toRelease(sanction),
            violations
        );
    }

    // API-ADM-06 제재 수동 해제
    @Transactional
    public SanctionReleaseResponseDTO release(long sanctionId, Long adminId, String reason) {
        String trimmed = reason == null ? "" : reason.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException(ErrorCode.RELEASE_REASON_REQUIRED);
        }
        if (trimmed.length() > MAX_RELEASE_REASON_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        Sanction sanction = sanctionRepository.findWithLockBySanctionId(sanctionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.SANCTION_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now().withNano(0);
        if (sanction.statusAt(now) != SanctionStatus.SUSPENDED) {
            throw new BusinessException(ErrorCode.SANCTION_NOT_RELEASABLE);
        }

        sanction.release(adminId, trimmed, now);

        long violationCount = violationRepository
            .countByCustomerIdAndViolationIdLessThanEqual(sanction.getCustomerId(), sanction.getViolationId());
        return new SanctionReleaseResponseDTO(
            sanction.getSanctionId(),
            sanction.statusAt(now),
            violationCount,
            toRelease(sanction)
        );
    }

    // API-ADM-07 위반 원문 조회
    @Transactional(readOnly = true)
    public ViolationOriginalDTO getOriginal(long violationId, Long adminId) {
        Violation violation = violationRepository.findById(violationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.VIOLATION_NOT_FOUND));
        Consultation consultation = consultationRepository.findById(violation.getConsultationId())
            .orElseThrow(() -> new IllegalStateException("위반 상담이 없습니다: " + violation.getConsultationId()));
        String customerLoginId = memberRepository.findById(violation.getCustomerId())
            .map(Member::getLoginId)
            .orElse(null);

        List<ViolationMessageDTO> messages = violationRepository
            .findOriginalMessages(violation.getConsultationId())
            .stream()
            .map(ViolationMessageDTO::from)
            .toList();

        LocalDateTime viewedAt = LocalDateTime.now().withNano(0);
        log.info("위반 원문 열람: adminId={}, violationId={}, viewedAt={}", adminId, violationId, viewedAt);

        return new ViolationOriginalDTO(
            violation.getViolationId(),
            violation.getConsultationId(),
            customerLoginId,
            consultation.getClosedBy(),
            violation.getCreatedAt(),
            messages,
            viewedAt
        );
    }

    private SanctionReleaseDTO toRelease(Sanction sanction) {
        if (sanction.getReleasedAt() == null) {
            return null;
        }
        PolicyAdminDTO releasedBy = sanction.getReleasedBy() == null ? null
            : memberRepository.findById(sanction.getReleasedBy()).map(PolicyAdminDTO::from).orElse(null);
        return new SanctionReleaseDTO(sanction.getReleasedAt(), releasedBy, sanction.getReleaseReason());
    }

    // 없으면 null(전체), 세 값이 아니면 400
    private String toStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return SanctionStatus.valueOf(status).name();
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
