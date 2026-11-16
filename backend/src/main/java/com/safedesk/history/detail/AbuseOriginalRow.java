package com.safedesk.history.detail;

// abuse_log 는 엔티티가 없어서 native SQL 로 읽는다.
// SQL 의 "as 별칭"과 getter 이름이 같으면 Spring Data 가 이 인터페이스에 자동으로 담아 준다.
public interface AbuseOriginalRow {
    Long getMessageId();
    String getOriginalContent();
}
