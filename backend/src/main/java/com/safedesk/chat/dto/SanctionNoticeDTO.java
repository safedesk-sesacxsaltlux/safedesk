package com.safedesk.chat.dto;

import java.time.LocalDateTime;

// CUS-01 403 CUSTOMER_SANCTIONED 의 data. 화면이 이용정지 안내(SCR-009)에 쓴다.
public record SanctionNoticeDTO(String reason, LocalDateTime releaseAt) {

}
