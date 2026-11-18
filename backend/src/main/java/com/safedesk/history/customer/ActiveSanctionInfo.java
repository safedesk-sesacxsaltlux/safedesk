package com.safedesk.history.customer;

import java.time.LocalDateTime;

// HIS-02 지금 정지 중인 제재 (native 쿼리 결과, getter 이름 = SQL "as 별칭")
public interface ActiveSanctionInfo {
    Long getSanctionId();
    Number getSuspendDays();
    LocalDateTime getStartAt();
    LocalDateTime getEndAt();
    String getCloseReason();        // 위반 상담의 종료 사유 (ABUSE_AUTO | ABUSE_MANUAL)
}
