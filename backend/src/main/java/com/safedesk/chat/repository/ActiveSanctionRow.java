package com.safedesk.chat.repository;

import java.time.LocalDateTime;

// CUS-01 정지 확인 쿼리 결과 (네이티브 쿼리 별칭과 이름이 같아야 한다)
public interface ActiveSanctionRow {

    LocalDateTime getReleaseAt();

    String getCloseReason();
}
