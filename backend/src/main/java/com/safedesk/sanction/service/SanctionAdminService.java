package com.safedesk.sanction.service;

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
import com.safedesk.sanction.dto.ViolationItemDTO;
import com.safedesk.sanction.entity.Sanction;
import com.safedesk.sanction.entity.SanctionStatus;
import com.safedesk.sanction.repository.SanctionRepository;
import com.safedesk.sanction.repository.ViolationHistoryRow;
import com.safedesk.sanction.repository.ViolationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 관리자 제재 관리 (API-ADM-04~07). 제재 생성(MOD-04)은 SanctionService 가 맡는다
@Service
public class SanctionAdminService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_DURATION_DAYS = 365;

    private final SanctionRepository sanctionRepository;
    private final ViolationRepository violationRepository;
    private final MemberRepository memberRepository;

    public SanctionAdminService(SanctionRepository sanctionRepository,
                                ViolationRepository violationRepository,
                                MemberRepository memberRepository) {
        this.sanctionRepository = sanctionRepository;
        this.violationRepository = violationRepository;
        this.memberRepository = memberRepository;
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
