package com.safedesk.sanction.service;

import com.safedesk.common.exception.BusinessException;
import com.safedesk.common.exception.ErrorCode;
import com.safedesk.common.response.PageResponse;
import com.safedesk.sanction.dto.SanctionListItemDTO;
import com.safedesk.sanction.entity.SanctionStatus;
import com.safedesk.sanction.repository.SanctionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 관리자 제재 관리 (API-ADM-04~07). 제재 생성(MOD-04)은 SanctionService 가 맡는다
@Service
public class SanctionAdminService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_DURATION_DAYS = 365;

    private final SanctionRepository sanctionRepository;

    public SanctionAdminService(SanctionRepository sanctionRepository) {
        this.sanctionRepository = sanctionRepository;
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
